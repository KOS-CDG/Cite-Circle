package com.citecircle.app.core.data

import com.citecircle.app.core.model.Post
import com.citecircle.app.core.model.PostFlair
import com.citecircle.app.core.model.PostType
import com.citecircle.app.core.model.User
import com.citecircle.app.core.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestorePostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
) : PostRepository {

    private val _localPosts = MutableStateFlow<List<Post>>(emptyList())

    override fun getFeedPosts(): Flow<List<Post>> = flow {
        val posts = try {
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                val authorId = doc.getString("authorId") ?: return@mapNotNull null
                val content = doc.getString("content") ?: return@mapNotNull null
                val typeStr = doc.getString("type") ?: PostType.DISCUSSION.name
                val flairStr = doc.getString("flair") ?: PostFlair.NONE.name
                val authorName = doc.getString("authorName") ?: "Scholar"
                val authorAvatar = doc.getString("authorAvatar") ?: ""

                Post(
                    id = doc.id,
                    author = User(
                        id = authorId,
                        name = authorName,
                        avatarUrl = authorAvatar,
                        role = UserRole.RESEARCHER,
                        institution = doc.getString("authorInstitution") ?: "CiteCircle Network"
                    ),
                    content = content,
                    type = runCatching { PostType.valueOf(typeStr) }.getOrDefault(PostType.DISCUSSION),
                    flair = runCatching { PostFlair.valueOf(flairStr) }.getOrDefault(PostFlair.NONE),
                    timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    endorseCount = (doc.getLong("endorseCount") ?: 0L).toInt(),
                    commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                    circleId = doc.getString("circleId"),
                    imageUrl = doc.getString("imageUrl")
                )
            }
            list.ifEmpty { FakeDataSource.posts }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FakeDataSource.posts
        }
        _localPosts.value = posts
        emit(posts)
    }

    override fun getSavedPosts(): Flow<List<Post>> = flow {
        emit(_localPosts.value.filter { it.isSaved })
    }

    override fun getPostById(id: String): Flow<Post?> = flow {
        val found = _localPosts.value.find { it.id == id } ?: try {
            val doc = firestore.collection("posts").document(id).get().await()
            if (doc.exists()) {
                val authorId = doc.getString("authorId") ?: "u_unknown"
                Post(
                    id = doc.id,
                    author = User(id = authorId, name = doc.getString("authorName") ?: "Scholar"),
                    content = doc.getString("content") ?: "",
                    timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) { null }
        emit(found)
    }

    override fun getPostsForCircle(circleId: String): Flow<List<Post>> = flow {
        val posts = try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("circleId", circleId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val content = doc.getString("content") ?: return@mapNotNull null
                Post(
                    id = doc.id,
                    author = User(id = doc.getString("authorId") ?: "", name = doc.getString("authorName") ?: "Scholar"),
                    content = content,
                    circleId = circleId,
                    timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) { emptyList() }
        emit(posts)
    }

    override suspend fun endorsePost(postId: String): Boolean {
        return try {
            val docRef = firestore.collection("posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("endorseCount") ?: 0L
                transaction.update(docRef, "endorseCount", currentCount + 1)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun savePost(postId: String): Boolean {
        val current = _localPosts.value.toMutableList()
        val idx = current.indexOfFirst { it.id == postId }
        if (idx >= 0) {
            val post = current[idx]
            current[idx] = post.copy(isSaved = !post.isSaved)
            _localPosts.value = current
        }
        return true
    }

    override suspend fun createPost(post: Post): Boolean {
        return try {
            var author = post.author
            userRepository.getCurrentUser().collect { current ->
                author = current
            }

            val docRef = firestore.collection("posts").document()
            docRef.set(
                mapOf(
                    "id" to docRef.id,
                    "authorId" to author.id,
                    "authorName" to author.name,
                    "authorAvatar" to author.avatarUrl,
                    "authorInstitution" to author.institution,
                    "content" to post.content,
                    "type" to post.type.name,
                    "flair" to post.flair.name,
                    "circleId" to post.circleId,
                    "imageUrl" to post.imageUrl,
                    "endorseCount" to 0,
                    "commentCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
