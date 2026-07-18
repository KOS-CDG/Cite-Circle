package com.citecircle.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

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
// Hilt Module
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindPostRepository(impl: FakePostRepository): PostRepository

    @Binds @Singleton
    abstract fun bindPaperRepository(impl: FakePaperRepository): PaperRepository

    @Binds @Singleton
    abstract fun bindCircleRepository(impl: FakeCircleRepository): CircleRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: FakeUserRepository): UserRepository

    @Binds @Singleton
    abstract fun bindCommentRepository(impl: FakeCommentRepository): CommentRepository

    @Binds @Singleton
    abstract fun bindMessageRepository(impl: FakeMessageRepository): MessageRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: FakeNotificationRepository): NotificationRepository

    @Binds @Singleton
    abstract fun bindAiReviewRepository(impl: FakeAiReviewRepository): AiReviewRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: FakeSearchRepository): SearchRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
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
}
