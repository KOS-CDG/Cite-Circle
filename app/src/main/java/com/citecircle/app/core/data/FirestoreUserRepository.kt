package com.citecircle.app.core.data

import com.citecircle.app.core.model.CoauthorGraphResponse
import com.citecircle.app.core.model.User
import com.citecircle.app.core.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val tokenManager: TokenManager,
    private val json: Json,
) : UserRepository {

    private val _currentUser = MutableStateFlow(FakeDataSource.currentUser)
    private val _pendingConnectionIds = MutableStateFlow(mutableSetOf<String>())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val savedUserJson = tokenManager.getUserJson()
            if (!savedUserJson.isNullOrBlank()) {
                runCatching {
                    val user = json.decodeFromString(User.serializer(), savedUserJson)
                    _currentUser.value = user
                }
            }
            tokenManager.getPendingConnectionsJson()?.let { jsonStr ->
                runCatching {
                    val set = json.decodeFromString(SetSerializer(String.serializer()), jsonStr)
                    _pendingConnectionIds.value = set.toMutableSet()
                }
            }
        }
    }

    override fun getCurrentUser(): Flow<User> = _currentUser.asStateFlow()

    override fun getUserById(id: String): Flow<User?> = flow {
        if (id == _currentUser.value.id) {
            emit(_currentUser.value)
            return@flow
        }
        val user = try {
            val doc = firestore.collection("users").document(id).get().await()
            if (doc.exists()) {
                User(
                    id = doc.id,
                    name = doc.getString("name") ?: "Scholar",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    role = runCatching { UserRole.valueOf(doc.getString("role") ?: "STUDENT") }.getOrDefault(UserRole.STUDENT),
                    institution = doc.getString("institution") ?: "",
                    fieldOfStudy = doc.getString("fieldOfStudy") ?: "",
                    bio = doc.getString("bio") ?: "",
                    orcidId = doc.getString("orcidId") ?: "",
                    followerCount = (doc.getLong("followerCount") ?: 0L).toInt(),
                    followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
                    citationCount = (doc.getLong("citationCount") ?: 0L).toInt(),
                    publicationCount = (doc.getLong("publicationCount") ?: 0L).toInt(),
                )
            } else {
                FakeDataSource.users.find { it.id == id }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FakeDataSource.users.find { it.id == id }
        }
        emit(user)
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        val users = try {
            val snapshot = firestore.collection("users").limit(20).get().await()
            snapshot.documents.map { doc ->
                User(
                    id = doc.id,
                    name = doc.getString("name") ?: "Scholar",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    role = runCatching { UserRole.valueOf(doc.getString("role") ?: "STUDENT") }.getOrDefault(UserRole.STUDENT),
                    institution = doc.getString("institution") ?: "CiteCircle Network"
                )
            }.ifEmpty { FakeDataSource.users }
        } catch (e: Exception) {
            FakeDataSource.users
        }
        emit(users)
    }

    override fun getSuggestedConnections(): Flow<List<User>> = flow {
        val users = try {
            val pending = _pendingConnectionIds.value
            val snapshot = firestore.collection("users").limit(10).get().await()
            snapshot.documents.map { doc ->
                val u = User(
                    id = doc.id,
                    name = doc.getString("name") ?: "Scholar",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    role = runCatching { UserRole.valueOf(doc.getString("role") ?: "STUDENT") }.getOrDefault(UserRole.STUDENT),
                    institution = doc.getString("institution") ?: "CiteCircle Network"
                )
                u.copy(connectionPending = pending.contains(u.id))
            }.ifEmpty {
                FakeDataSource.users.filter { !it.isConnected && !it.connectionPending }.take(8)
            }
        } catch (e: Exception) {
            FakeDataSource.users.filter { !it.isConnected && !it.connectionPending }.take(8)
        }
        emit(users)
    }

    override fun getConnectionRequests(): Flow<List<User>> = flow {
        emit(FakeDataSource.connectionRequests)
    }

    override suspend fun followUser(userId: String): Boolean {
        val current = _currentUser.value
        val isFollowing = current.followingCount > 0
        val updated = current.copy(
            followingCount = if (isFollowing) (current.followingCount - 1).coerceAtLeast(0) else current.followingCount + 1
        )
        updateCurrentUser(updated)
        return true
    }

    override suspend fun connectUser(userId: String): Boolean {
        val current = _pendingConnectionIds.value.toMutableSet()
        current.add(userId)
        _pendingConnectionIds.value = current
        runCatching {
            tokenManager.savePendingConnectionsJson(
                json.encodeToString(SetSerializer(String.serializer()), current)
            )
        }
        return true
    }

    override suspend fun acceptConnection(userId: String): Boolean {
        val current = _pendingConnectionIds.value.toMutableSet()
        current.remove(userId)
        _pendingConnectionIds.value = current
        return true
    }

    override suspend fun declineConnection(userId: String): Boolean {
        val current = _pendingConnectionIds.value.toMutableSet()
        current.remove(userId)
        _pendingConnectionIds.value = current
        return true
    }

    override suspend fun updateCurrentUser(user: User): Boolean {
        _currentUser.value = user
        runCatching {
            tokenManager.saveUserJson(json.encodeToString(User.serializer(), user))
        }

        // Push updated user document to Cloud Firestore
        runCatching {
            firestore.collection("users").document(user.id).set(
                mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "avatarUrl" to user.avatarUrl,
                    "coverUrl" to user.coverUrl,
                    "role" to user.role.name,
                    "institution" to user.institution,
                    "fieldOfStudy" to user.fieldOfStudy,
                    "bio" to user.bio,
                    "orcidId" to user.orcidId,
                    "followerCount" to user.followerCount,
                    "followingCount" to user.followingCount,
                    "citationCount" to user.citationCount,
                    "publicationCount" to user.publicationCount,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
        return true
    }

    override fun getCoauthorGraph(userId: String): Flow<CoauthorGraphResponse> = flow {
        var fallback: CoauthorGraphResponse? = null
        FakeUserRepository().getCoauthorGraph(userId).collect { fallback = it }
        emit(fallback ?: CoauthorGraphResponse())
    }
}
