# Comprehensive System Settings & Multi-Account Management Prompt

This document provides a comprehensive, production-grade prompt and architectural specification for implementing a **Functional System Settings Screen** in modern Android applications built with **Kotlin**, **Jetpack Compose**, **DataStore**, **Hilt**, and **Coroutines Flow**.

---

## 🚀 Master Prompt for AI Coding Assistants

```markdown
Role: Senior Android Architect & Jetpack Compose Engineer

Task:
Build a complete, fully functional System Settings screen and Multi-Account Management system for an Android application using Jetpack Compose, Hilt, DataStore Preferences, and Kotlin Coroutines/Flow.

Features & Requirements:

1. Account & Security Credentials:
   - Change Password: Modal dialog requiring Current Password, New Password, and Confirm New Password with password strength validation (minimum 6 characters) and matching password checks.
   - Change Email Address: Modal dialog taking New Email Address and Current Password for identity verification. Updates local DataStore & UserRepository.
   - Edit Profile Navigation: Direct link to the Edit Profile screen.

2. Multi-Account Management & Switcher:
   - Persist multiple signed-in accounts in DataStore (`SavedAccount` containing userId, email, name, avatarUrl, role, accessToken, refreshToken, isActive).
   - Visual Account Switcher list showing user avatar, name, email, role, and an "Active" badge. Tapping any non-active account instantly switches the active auth token and refreshes the user repository without restarting the app.
   - Add Account: Modal dialog allowing users to enter secondary account credentials (email and password), authenticate, save the session, and set it as active.
   - Remove Account: Ability to remove secondary account sessions from the device.

3. Appearance & Theme Preferences:
   - Theme toggle chips (Light Mode, Dark Mode, System Default) bound to a ThemeRepository using DataStore.

4. Notification & Communication Preferences:
   - Interactive toggle switches for Endorsements, Comments, Connection Requests, and Citation/AI Alerts.
   - Email Digest Frequency selector chips ("Instant", "Daily", "Weekly", "Off").

5. System Storage & Cache Management:
   - Clear App Cache: Action button to wipe temporary PDF files, paper summary cache, and local search indices with size indication (e.g. ~14.2 MB cleared) and confirmation dialog.
   - Export Data: Option to generate user data export summaries.

6. Session Logout Controls:
   - "Log Out Current Account": Signs out the active account and switches to another saved account or navigates to the Auth screen if no accounts remain.
   - "Log Out All Accounts": Wipes all saved account sessions, tokens, and local cached data, then navigates to the Auth screen.

Aesthetics & UX:
   - Material 3 Design System with clean card containers (`CcCard`), typography hierarchy, custom action rows (`SettingsActionRow`), floating top app bar with back navigation, animated feedback, and Snackbar notifications.

Deliverables:
1. `SavedAccount` data model.
2. DataStore preference keys in `TokenManager`.
3. `AuthRepository` interface methods & concrete implementations (`RealAuthRepository` & `FakeAuthRepository`).
4. `SettingsViewModel` with reactive state (`SettingsUiState`, `ActiveSettingsDialog`).
5. `SettingsScreen` Jetpack Compose UI with all modal dialogs.
6. Navigation integration in `NavGraph`.
```

---

## 📐 Architecture & Data Flow

```
                                  ┌────────────────────────┐
                                  │   SettingsScreen UI    │
                                  └───────────┬────────────┘
                                              │
                                   Observe State / Dispatch Actions
                                              │
                                              ▼
                                  ┌────────────────────────┐
                                  │   SettingsViewModel    │
                                  └───────────┬────────────┘
                                              │
                                    Triggers Coroutines
                                              │
                                              ▼
                                  ┌────────────────────────┐
                                  │     AuthRepository     │
                                  └─────┬──────────────┬───┘
                                        │              │
                                        ▼              ▼
                     ┌────────────────────┐          ┌──────────────────────┐
                     │    TokenManager    │          │    UserRepository    │
                     │ (DataStore Presets)│          │  (StateFlow User)    │
                     └────────────────────┘          └──────────────────────┘
```

---

## 🛠️ Data Models & Interface Contracts

### 1. SavedAccount Data Model (`Models.kt`)

```kotlin
@Serializable
data class SavedAccount(
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String = "",
    val role: String = "STUDENT",
    val accessToken: String = "",
    val refreshToken: String = "",
    val isActive: Boolean = false
)
```

### 2. AuthRepository Interface Contract (`Repositories.kt`)

```kotlin
interface AuthRepository {
    fun isLoggedIn(): Boolean
    suspend fun login(email: String, password: String): Boolean
    suspend fun signup(email: String, password: String, role: UserRole): Boolean
    suspend fun logout()
    
    // System Settings & Account Management Extensions
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    suspend fun changeEmail(newEmail: String, currentPassword: String): Result<Unit>
    fun getSavedAccounts(): Flow<List<SavedAccount>>
    suspend fun switchAccount(userId: String): Boolean
    suspend fun addAccount(email: String, password: String): Result<Boolean>
    suspend fun removeAccount(userId: String): Boolean
    suspend fun clearCache(): Boolean
    suspend fun logoutAll()
}
```

---

## 📱 Implementation Verification & Test Matrix

| Feature Component | Test Case / User Flow | Expected Result |
| :--- | :--- | :--- |
| **Change Password** | User opens Change Password dialog, enters valid old & new passwords. | Validated locally, updates credential status, shows success snackbar, dismisses dialog. |
| **Change Email** | User enters new email and confirms with current password. | Updates saved email preference in DataStore and triggers confirmation snackbar. |
| **Switch Account** | User taps an inactive saved account in the Account Switcher list. | `TokenManager` updates active token and `UserRepository` immediately emits updated user. |
| **Add Account** | User taps "Add Account", enters secondary email & password. | Secondary account is authenticated, stored in DataStore list, and marked active. |
| **Clear Cache** | User taps "Clear App Cache & Data" and confirms in dialog. | Clears temporary summary cache & search indices; displays size cleared notice. |
| **Log Out Current** | User taps "Log Out Current Account". | Removes current session, clears tokens, and navigates to `AuthRoute`. |
| **Log Out All** | User taps "Log Out of All Accounts" and confirms. | Clears all saved sessions and DataStore preferences, then redirects to `AuthRoute`. |

---

## 🎯 Code Structure Summary

- **`TokenManager.kt`**: Handles DataStore persistence for tokens, active user email, and JSON stringified list of `SavedAccount` sessions.
- **`RealRepositories.kt`**: `RealAuthRepository` implements multi-account persistence, switching logic, token updating, and cache cleanup.
- **`SettingsScreen.kt`**: Contains `SettingsViewModel` with reactive state flow, `SettingsScreen` UI composable with cards for Account, Switcher, Theme, Notifications, Storage, and Logout, plus 5 dedicated Compose dialogs (`ChangePasswordDialog`, `ChangeEmailDialog`, `AddAccountDialog`, `ConfirmClearCache`, `ConfirmLogoutAll`).
- **`ProfileScreen.kt`**: Header updated with a Settings gear icon button adjacent to the Edit Profile button.
- **`NavGraph.kt`**: Connects `SettingsRoute` with navigation callbacks to `EditProfileRoute`, `AuthRoute`, and backstack pop.
