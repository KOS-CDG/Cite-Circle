# Prompt 1: System Settings & Multi-Account Management System

```markdown
Role: Senior Android Architect & Jetpack Compose Engineer

Task:
Build a complete, production-grade System Settings screen and Multi-Account Management system for Cite Circle using Jetpack Compose, Hilt, DataStore Preferences, and Kotlin Coroutines/Flow.

Features & Requirements:

1. Account Credentials & Security:
   - Change Password Dialog: Input fields for Current Password, New Password, and Confirm New Password with real-time strength validation (>=6 chars, uppercase/number requirement) and matching checks.
   - Change Email Dialog: Input fields for New Email Address and Current Password for identity re-authentication. Updates local DataStore & user profile state.
   - Profile Shortcut: Quick action link to navigate to the Edit Profile screen.

2. Multi-Account Management & Switcher:
   - DataStore Persistence: Store multiple signed-in accounts in DataStore using a serializable list of `SavedAccount` objects (userId, email, name, avatarUrl, role, accessToken, refreshToken, isActive).
   - Visual Account Switcher Card: Display all signed-in user profiles with avatar thumbnail, display name, email, academic role tag, and an "Active" badge.
   - Instant Switch Action: Tapping an inactive saved account updates `TokenManager` active session tokens and refreshes `UserRepository` immediately without restarting the application.
   - Add Account Dialog: Secondary authentication modal allowing users to log into another existing account, persist credentials, and activate the session.
   - Remove Account Action: Ability to swipe/delete a secondary account session from local storage.

3. Appearance & Design Preferences:
   - Theme Selector Chips: Light Mode, Dark Mode, System Default bound reactively to a `ThemeRepository` leveraging DataStore preferences.

4. Notification & Communication Matrix:
   - Toggle Switches: Endorsements, Comments, Connection Requests, Paper Citations, AI Copilot notifications.
   - Digest Frequency Chips: Instant, Daily, Weekly, Off.

5. System Storage & Cache Management:
   - Cache Cleaner: Action button to calculate local cached storage (PDF files, paper summaries, thumbnail cache) and clear files upon user confirmation in a dialog, displaying a size cleared notice (e.g. "~14.2 MB cleared").
   - Data Export: Generate and download a JSON user data summary.

6. Session Logout Controls:
   - Log Out Active Account: Invalidates current token, removes session from `SavedAccount` list, and automatically switches to another saved account or redirects to `AuthRoute` if no accounts remain.
   - Log Out All Accounts: Clears all DataStore preference entries, invalidates all saved sessions, and resets application state to `AuthRoute`.

Deliverables:
1. `SavedAccount` Kotlin data model & `TokenManager` DataStore extensions.
2. `AuthRepository` interface methods for multi-account switching and cache operations.
3. `SettingsViewModel` managing reactive `SettingsUiState` and dialog visibility states.
4. `SettingsScreen` Jetpack Compose UI component utilizing Material 3 components.
5. Navigation integration in `NavGraph`.
```
