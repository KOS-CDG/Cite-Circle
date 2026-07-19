package com.citecircle.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Supabase auth tokens in DataStore for automatic injection into
 * network requests via the OkHttp AuthInterceptor.
 */
private val Context.tokenDataStore by preferencesDataStore(name = "cite_circle_auth")

@Singleton
class TokenManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_JSON = stringPreferencesKey("user_json")
        private val KEY_AI_MESSAGES_JSON = stringPreferencesKey("ai_messages_json")
        private val KEY_SAVED_PAPERS_JSON = stringPreferencesKey("saved_papers_json")
        private val KEY_SHELVES_JSON = stringPreferencesKey("shelves_json")
        private val KEY_JOINED_CIRCLES_JSON = stringPreferencesKey("joined_circles_json")
        private val KEY_PENDING_CONNECTIONS_JSON = stringPreferencesKey("pending_connections_json")
        private val KEY_SAVED_ACCOUNTS_JSON = stringPreferencesKey("saved_accounts_json")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    }

    /** Persist tokens after login/signup. */
    suspend fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
        }
    }

    /** Clear tokens on logout. */
    suspend fun clearTokens() {
        context.tokenDataStore.edit { it.clear() }
    }

    /** Read the current access token (blocking-safe for interceptors). */
    suspend fun getAccessToken(): String? =
        context.tokenDataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()

    /** Reactive stream of the current user ID. */
    fun getUserId(): Flow<String?> =
        context.tokenDataStore.data.map { it[KEY_USER_ID] }

    /** One-shot read of user ID. */
    suspend fun getCurrentUserId(): String? =
        context.tokenDataStore.data.map { it[KEY_USER_ID] }.first()

    /** True if we have a stored access token. */
    suspend fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    // ── Local persistence helpers ──

    suspend fun saveUserJson(json: String) {
        context.tokenDataStore.edit { it[KEY_USER_JSON] = json }
    }

    suspend fun getUserJson(): String? =
        context.tokenDataStore.data.map { it[KEY_USER_JSON] }.first()

    suspend fun saveAiMessagesJson(json: String) {
        context.tokenDataStore.edit { it[KEY_AI_MESSAGES_JSON] = json }
    }

    suspend fun getAiMessagesJson(): String? =
        context.tokenDataStore.data.map { it[KEY_AI_MESSAGES_JSON] }.first()

    suspend fun saveSavedPapersJson(json: String) {
        context.tokenDataStore.edit { it[KEY_SAVED_PAPERS_JSON] = json }
    }

    suspend fun getSavedPapersJson(): String? =
        context.tokenDataStore.data.map { it[KEY_SAVED_PAPERS_JSON] }.first()

    suspend fun saveShelvesJson(json: String) {
        context.tokenDataStore.edit { it[KEY_SHELVES_JSON] = json }
    }

    suspend fun getShelvesJson(): String? =
        context.tokenDataStore.data.map { it[KEY_SHELVES_JSON] }.first()

    suspend fun saveJoinedCirclesJson(json: String) {
        context.tokenDataStore.edit { it[KEY_JOINED_CIRCLES_JSON] = json }
    }

    suspend fun getJoinedCirclesJson(): String? =
        context.tokenDataStore.data.map { it[KEY_JOINED_CIRCLES_JSON] }.first()

    suspend fun savePendingConnectionsJson(json: String) {
        context.tokenDataStore.edit { it[KEY_PENDING_CONNECTIONS_JSON] = json }
    }

    suspend fun getPendingConnectionsJson(): String? =
        context.tokenDataStore.data.map { it[KEY_PENDING_CONNECTIONS_JSON] }.first()

    suspend fun saveSavedAccountsJson(json: String) {
        context.tokenDataStore.edit { it[KEY_SAVED_ACCOUNTS_JSON] = json }
    }

    suspend fun getSavedAccountsJson(): String? =
        context.tokenDataStore.data.map { it[KEY_SAVED_ACCOUNTS_JSON] }.first()

    suspend fun saveUserEmail(email: String) {
        context.tokenDataStore.edit { it[KEY_USER_EMAIL] = email }
    }

    suspend fun getUserEmail(): String? =
        context.tokenDataStore.data.map { it[KEY_USER_EMAIL] }.first()
}
