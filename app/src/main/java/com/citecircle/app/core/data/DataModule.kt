package com.citecircle.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.citecircle.app.BuildConfig
import com.citecircle.app.core.network.CiteCircleApi
import com.citecircle.app.core.network.FireworksApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────────────
// Named qualifiers to distinguish the two OkHttpClients
// ──────────────────────────────────────────────────────────────────────────────

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FireworksClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CiteCircleClient

// ──────────────────────────────────────────────────────────────────────────────
// Theme preference (DataStore)
// ──────────────────────────────────────────────────────────────────────────────

enum class AppTheme { LIGHT, DARK, SYSTEM }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cite_circle_prefs")
private val THEME_KEY = stringPreferencesKey("app_theme")

interface ThemeRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}

class DataStoreThemeRepository(private val dataStore: DataStore<Preferences>) : ThemeRepository {
    override fun getTheme(): Flow<AppTheme> = dataStore.data.map { prefs ->
        AppTheme.valueOf(prefs[THEME_KEY] ?: AppTheme.SYSTEM.name)
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[THEME_KEY] = theme.name }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Hilt Module — Repository bindings (Real implementations)
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindPostRepository(impl: RealPostRepository): PostRepository

    @Binds @Singleton
    abstract fun bindPaperRepository(impl: RealPaperRepository): PaperRepository

    @Binds @Singleton
    abstract fun bindCircleRepository(impl: RealCircleRepository): CircleRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: RealUserRepository): UserRepository

    @Binds @Singleton
    abstract fun bindCommentRepository(impl: RealCommentRepository): CommentRepository

    @Binds @Singleton
    abstract fun bindMessageRepository(impl: RealMessageRepository): MessageRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: RealNotificationRepository): NotificationRepository

    @Binds @Singleton
    abstract fun bindAiReviewRepository(impl: RealAiReviewRepository): AiReviewRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: RealSearchRepository): SearchRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: RealAuthRepository): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides @Singleton
    fun provideThemeRepository(dataStore: DataStore<Preferences>): ThemeRepository =
        DataStoreThemeRepository(dataStore)

    @Provides @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager =
        TokenManager(context)
}

// ──────────────────────────────────────────────────────────────────────────────
// Network Rate Limiter Interceptor
// ──────────────────────────────────────────────────────────────────────────────

class RateLimitInterceptor(
    private val requestsPerMinute: Int = 60,
    private val minIntervalMs: Long = 50L
) : Interceptor {
    private val lock = Any()
    private var lastRequestTime = 0L
    private val requestTimestamps = ArrayDeque<Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(lock) {
            val now = System.currentTimeMillis()

            // 1. Clean timestamps older than 60 seconds (1 minute window)
            while (requestTimestamps.isNotEmpty() && (now - requestTimestamps.first) > 60000L) {
                requestTimestamps.removeFirst()
            }

            // 2. Enforce Requests Per Minute limit
            if (requestTimestamps.size >= requestsPerMinute) {
                val oldestInWindow = requestTimestamps.first
                val waitMs = 60000L - (now - oldestInWindow)
                if (waitMs > 0) {
                    try {
                        Thread.sleep(waitMs)
                    } catch (_: InterruptedException) {}
                }
            }

            // 3. Enforce minimum interval between consecutive requests
            val currentTime = System.currentTimeMillis()
            val elapsedSinceLast = currentTime - lastRequestTime
            if (elapsedSinceLast < minIntervalMs) {
                val delayNeeded = minIntervalMs - elapsedSinceLast
                try {
                    Thread.sleep(delayNeeded)
                } catch (_: InterruptedException) {}
            }

            val executionTime = System.currentTimeMillis()
            lastRequestTime = executionTime
            requestTimestamps.addLast(executionTime)
        }

        return chain.proceed(chain.request())
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Network Module — Fireworks.ai (AI paper summaries)
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val FIREWORKS_BASE_URL = "https://api.fireworks.ai/inference/v1/"

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides @Singleton @FireworksClient
    fun provideFireworksOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(RateLimitInterceptor(requestsPerMinute = 60, minIntervalMs = 50L))
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.FIREWORKS_API_KEY}")
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides @Singleton
    fun provideFireworksApi(@FireworksClient client: OkHttpClient, json: Json): FireworksApi =
        Retrofit.Builder()
            .baseUrl(FIREWORKS_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FireworksApi::class.java)
}

// ──────────────────────────────────────────────────────────────────────────────
// Network Module — Cite Circle Backend (Render)
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object CiteCircleNetworkModule {
    private const val CITE_CIRCLE_BASE_URL = "https://cite-circle.onrender.com/"

    @Provides @Singleton @CiteCircleClient
    fun provideCiteCircleOkHttpClient(tokenManager: TokenManager): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = runBlocking { tokenManager.getAccessToken() }
                val builder = chain.request().newBuilder()
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .build()

    @Provides @Singleton
    fun provideCiteCircleApi(@CiteCircleClient client: OkHttpClient, json: Json): CiteCircleApi =
        Retrofit.Builder()
            .baseUrl(CITE_CIRCLE_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CiteCircleApi::class.java)
}
