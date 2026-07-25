package com.citecircle.app.core.data

import com.citecircle.app.core.model.SavedAccount
import com.citecircle.app.core.model.User
import com.citecircle.app.core.model.UserRole
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val json: Json,
) : AuthRepository {

    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getSavedAccountsJson()?.let { jsonStr ->
                runCatching {
                    val list = json.decodeFromString(ListSerializer(SavedAccount.serializer()), jsonStr)
                    _savedAccounts.value = list
                }
            }
        }
    }

    private fun persistAccounts(accounts: List<SavedAccount>) {
        _savedAccounts.value = accounts
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                tokenManager.saveSavedAccountsJson(json.encodeToString(ListSerializer(SavedAccount.serializer()), accounts))
            }
        }
    }

    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null || runBlocking { tokenManager.isLoggedIn() }
    }

    override suspend fun login(email: String, password: String): Boolean {
        // Explicit demo bypass ONLY for hardcoded demo buttons
        if ((email == "admin@citecircle.com" || email == "dummy@citecircle.com" || email == "orcid@orcid.org" || email == "google@gmail.com") && password == "password") {
            val nameFromEmail = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val demoUser = User(
                id = if (email.startsWith("admin")) "u_admin" else "u_${System.currentTimeMillis()}",
                name = nameFromEmail,
                role = UserRole.STUDENT,
                institution = "CiteCircle Network"
            )
            tokenManager.saveTokens("demo_access_token", "demo_refresh_token", demoUser.id)
            tokenManager.saveUserEmail(email)
            userRepository.updateCurrentUser(demoUser)
            addSavedAccountSession(demoUser, email, "demo_access_token", "demo_refresh_token")
            return true
        }

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return false
            val uid = firebaseUser.uid

            tokenManager.saveTokens(uid, "firebase_refresh_token", uid)
            tokenManager.saveUserEmail(email)

            // Attempt to load profile document from Firestore
            val userProfile = try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: email.substringBefore("@")
                    val roleStr = doc.getString("role") ?: UserRole.STUDENT.name
                    val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.STUDENT)
                    val institution = doc.getString("institution") ?: "CiteCircle Network"
                    val avatarUrl = doc.getString("avatarUrl") ?: "https://api.dicebear.com/8.x/avataaars/svg?seed=$name"
                    
                    User(
                        id = uid,
                        name = name,
                        avatarUrl = avatarUrl,
                        role = role,
                        institution = institution
                    )
                } else null
            } catch (e: Exception) {
                null
            }

            val finalUser = userProfile ?: User(
                id = uid,
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = UserRole.STUDENT,
                institution = "CiteCircle Network"
            )

            userRepository.updateCurrentUser(finalUser)
            addSavedAccountSession(finalUser, email, uid, "firebase_refresh_token")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Strictly fail authentication on invalid password or missing account
            false
        }
    }

    override suspend fun signup(email: String, password: String, role: UserRole): Boolean {
        val nameFromEmail = email.substringBefore("@")
            .split(".")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return false
            val uid = firebaseUser.uid

            val newUser = User(
                id = uid,
                name = nameFromEmail,
                avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=$nameFromEmail",
                role = role,
                institution = "CiteCircle Network"
            )

            // Save User Record to Firestore /users/{uid}
            runCatching {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "id" to uid,
                        "email" to email,
                        "name" to nameFromEmail,
                        "role" to role.name,
                        "institution" to "CiteCircle Network",
                        "avatarUrl" to newUser.avatarUrl,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            tokenManager.saveTokens(uid, "firebase_refresh_token", uid)
            tokenManager.saveUserEmail(email)
            userRepository.updateCurrentUser(newUser)
            addSavedAccountSession(newUser, email, uid, "firebase_refresh_token")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Strictly fail signup on invalid credentials or error
            false
        }
    }


    override suspend fun logout() {
        runCatching { firebaseAuth.signOut() }
        val currentUserId = tokenManager.getCurrentUserId()
        val accounts = _savedAccounts.value
        val remaining = accounts.filter { it.userId != currentUserId }

        if (remaining.isNotEmpty()) {
            val nextActive = remaining.first()
            tokenManager.saveTokens(nextActive.accessToken, nextActive.refreshToken, nextActive.userId)
            tokenManager.saveUserEmail(nextActive.email)

            val updated = remaining.map { acc ->
                acc.copy(isActive = (acc.userId == nextActive.userId))
            }
            persistAccounts(updated)

            val switchedUser = User(
                id = nextActive.userId,
                name = nextActive.name,
                avatarUrl = nextActive.avatarUrl,
                role = runCatching { UserRole.valueOf(nextActive.role) }.getOrDefault(UserRole.STUDENT),
                institution = "CiteCircle Network"
            )
            userRepository.updateCurrentUser(switchedUser)
        } else {
            persistAccounts(emptyList())
            tokenManager.clearTokens()
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Result.failure(Exception("No authenticated user found"))

        return try {
            val email = user.email ?: return Result.failure(Exception("User email unavailable"))
            val credential = EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changeEmail(newEmail: String, currentPassword: String): Result<Unit> {
        val user = firebaseAuth.currentUser
            ?: return Result.failure(Exception("No authenticated user found"))

        return try {
            val oldEmail = user.email ?: return Result.failure(Exception("Current email unavailable"))
            val credential = EmailAuthProvider.getCredential(oldEmail, currentPassword)
            user.reauthenticate(credential).await()
            user.updateEmail(newEmail).await()
            tokenManager.saveUserEmail(newEmail)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedAccounts(): Flow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    override suspend fun switchAccount(userId: String): Boolean {
        val accounts = _savedAccounts.value
        val target = accounts.find { it.userId == userId } ?: return false

        tokenManager.saveTokens(target.accessToken, target.refreshToken, target.userId)
        tokenManager.saveUserEmail(target.email)

        val updated = accounts.map { acc ->
            acc.copy(isActive = (acc.userId == userId))
        }
        persistAccounts(updated)

        val switchedUser = User(
            id = target.userId,
            name = target.name,
            avatarUrl = target.avatarUrl,
            role = runCatching { UserRole.valueOf(target.role) }.getOrDefault(UserRole.STUDENT),
            institution = "CiteCircle Network"
        )
        userRepository.updateCurrentUser(switchedUser)
        return true
    }

    override suspend fun addAccount(email: String, password: String): Result<Boolean> {
        if (!email.contains("@") || password.length < 4) {
            return Result.failure(Exception("Please enter a valid email and password."))
        }
        val success = login(email, password)
        return if (success) Result.success(true) else Result.failure(Exception("Authentication failed for $email"))
    }

    override suspend fun removeAccount(userId: String): Boolean {
        val updated = _savedAccounts.value.filter { it.userId != userId }
        persistAccounts(updated)
        return true
    }

    override suspend fun clearCache(): Boolean {
        delay(400)
        return true
    }

    override suspend fun exportUserData(): Result<String> {
        delay(500)
        val activeAcc = _savedAccounts.value.find { it.isActive }
        val jsonSummary = """
            {
              "exportTimestamp": ${System.currentTimeMillis()},
              "appName": "CiteCircle",
              "savedAccountsCount": ${_savedAccounts.value.size},
              "activeUser": {
                "userId": "${activeAcc?.userId ?: ""}",
                "name": "${activeAcc?.name ?: ""}",
                "email": "${activeAcc?.email ?: ""}",
                "role": "${activeAcc?.role ?: ""}"
              }
            }
        """.trimIndent()
        return Result.success(jsonSummary)
    }

    override suspend fun logoutAll() {
        runCatching { firebaseAuth.signOut() }
        persistAccounts(emptyList())
        tokenManager.clearAll()
    }

    private fun addSavedAccountSession(user: User, email: String, access: String, refresh: String) {
        val current = _savedAccounts.value.map { it.copy(isActive = false) }.toMutableList()
        val existingIdx = current.indexOfFirst { it.userId == user.id }
        val account = SavedAccount(
            userId = user.id,
            email = email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            role = user.role.name,
            accessToken = access,
            refreshToken = refresh,
            isActive = true
        )
        if (existingIdx >= 0) {
            current[existingIdx] = account
        } else {
            current.add(0, account)
        }
        persistAccounts(current)
    }
}
