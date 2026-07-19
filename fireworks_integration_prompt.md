# Prompt for Claude Fable: Implement Fireworks.ai Features in CiteCircle

You are an autonomous senior Android engineer tasked with implementing AI-powered features for **CiteCircle**—a professional social networking and publishing platform for academic researchers. The app is written in **Kotlin** and uses **Jetpack Compose** for the UI, **Dagger Hilt** for dependency injection, **Retrofit/OkHttp** for network requests, and **Kotlinx Serialization** for JSON parsing.

Your goal is to replace the simulated `FakeAiReviewRepository` with a real network-based implementation that communicates with **Fireworks.ai** using their REST Chat Completions API.

---

## 1. Codebase Context & Key Files

Here are the relevant files in the project that you need to read and update:
* **Domain Models**: [Models.kt](file:///home/dhale/Downloads/Projects/Projects/Research_APP/app/src/main/java/com/citecircle/app/core/model/Models.kt) (Defines `PaperDraft`, `AiReviewReport`, `AiSuggestion`, `Severity`, and `AiReviewStage`).
* **Repository Definitions & Fakes**: [Repositories.kt](file:///home/dhale/Downloads/Projects/Projects/Research_APP/app/src/main/java/com/citecircle/app/core/data/Repositories.kt) (See `AiReviewRepository` interface, `FakeAiReviewRepository`, and `AiReviewStage`).
* **Hilt Injection Configuration**: [DataModule.kt](file:///home/dhale/Downloads/Projects/Projects/Research_APP/app/src/main/java/com/citecircle/app/core/data/DataModule.kt) (Binds interfaces to their implementations).
* **Dependency Catalog**: [libs.versions.toml](file:///home/dhale/Downloads/Projects/Projects/Research_APP/gradle/libs.versions.toml) (Contains library versions).
* **App Gradle File**: [build.gradle.kts](file:///home/dhale/Downloads/Projects/Projects/Research_APP/app/build.gradle.kts).
* **UI Flow**: [PublishFlowScreen.kt](file:///home/dhale/Downloads/Projects/Projects/Research_APP/app/feature/publish/PublishFlowScreen.kt) (Shows how `PublishViewModel` triggers `reviewPaper(draft)` and listens to `getReviewProgress()`).

---

## 2. Requirements & Fireworks.ai Specifications

1. **Endpoint**: `https://api.fireworks.ai/inference/v1/chat/completions`
2. **Method**: `POST`
3. **Authorization Header**: `Authorization: Bearer <FIREWORKS_API_KEY>`
4. **Model to Use**: `accounts/fireworks/models/llama-v3p1-70b-instruct` (or a similar high-performance open-weight model suited for structured text extraction).
5. **Structured Outputs**: Use Fireworks.ai's JSON Mode by adding `"response_format": {"type": "json_object"}` to the request body. You must direct the model via the system prompt to return a JSON payload matching the target `AiReviewReport` schema exactly.
6. **API Key Security**: The API key must not be hardcoded. Load it dynamically from a Gradle `BuildConfig` field supplied via `local.properties` (e.g., `fireworks.api.key=YOUR_KEY`), or via a secure resource config.
7. **Loading & Progress UX**: In the UI, `Step3AiReview` expects progressive loading messages as the check proceeds. The new repository implementation should update the progress state flow with realistic milestones:
   - "Reading manuscript..."
   - "Sending data to Fireworks.ai..."
   - "Analyzing structure & clarity..."
   - "Scanning citations..."
   - "Generating recommendations..."
   - "Complete" (on success) or "Error" (on failure).

---

## 3. Structured JSON Schema
The API must return a JSON response matching this schema:

```json
{
  "score": 85,          // Int (overall score 0-100)
  "structure": 90,      // Int (0-100)
  "citations": 75,      // Int (0-100)
  "clarity": 88,        // Int (0-100)
  "originality": 82,    // Int (0-100)
  "suggestions": [
    {
      "id": "s1",
      "section": "Abstract", // E.g., Abstract, Related Work, Methodology, Results, Discussion, Conclusion
      "text": "The abstract does not quantitatively detail main findings...",
      "severity": "MODERATE", // MINOR, MODERATE, NEEDS_ATTENTION
      "isAddressed": false
    }
  ]
}
```

---

## 4. Step-by-Step Tasks to Complete

### Task 4.1: Add Gradle Dependencies
1. In `gradle/libs.versions.toml`, add definitions for:
   - Retrofit: `com.squareup.retrofit2:retrofit`
   - OkHttp Logging Interceptor: `com.squareup.okhttp3:logging-interceptor`
   - Kotlinx Serialization Converter: `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter`
2. Add these to `app/build.gradle.kts`.
3. In `app/build.gradle.kts`, enable buildFeatures `buildConfig = true` to pass the API Key from `local.properties`.

### Task 4.2: Add API Key Configuration
1. Show how to read a `fireworks.api.key` property in `app/build.gradle.kts` and place it in the `BuildConfig` class:
   ```kotlin
   val localProperties = java.util.Properties().apply {
       val file = rootProject.file("local.properties")
       if (file.exists()) {
           load(file.inputStream())
       }
   }
   val fireworksApiKey = localProperties.getProperty("fireworks.api.key") ?: "\"\""
   buildTypes {
       release {
           buildConfigField("String", "FIREWORKS_API_KEY", fireworksApiKey)
       }
       debug {
           buildConfigField("String", "FIREWORKS_API_KEY", fireworksApiKey)
       }
   }
   ```

### Task 4.3: Define Request and Response Models
Create a new package or file `com.citecircle.app.core.network` with serializable request/response objects for Fireworks.ai chat completions:
* `FireworksChatRequest` containing messages, model, temperature, and `response_format`.
* `FireworksChatResponse` that represents the standard chat completions payload with nested options (`choices`, `message`).

### Task 4.4: Create the Retrofit Service
Define a `FireworksApiService` Retrofit interface:
```kotlin
interface FireworksApiService {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Body request: FireworksChatRequest
    ): FireworksChatResponse
}
```

### Task 4.5: Implement the Network Repository
Create `FireworksAiReviewRepository` implementing `AiReviewRepository` in `com.citecircle.app.core.data`:
1. Use a `MutableStateFlow` to emit the progressive status messages in `getReviewProgress()`.
2. Construct the prompt with the paper's title and abstract.
3. Formulate the system instruction:
   * "You are a professional peer reviewer for academic manuscripts. Critique the provided title and abstract on Structure, Citations, Clarity, and Originality. Provide an overall score (0-100) and scores for each criterion. Also supply 3 to 6 constructive, actionable suggestions (AiSuggestion) categorizing their severity (MINOR, MODERATE, NEEDS_ATTENTION). You must output your response in valid JSON matching this schema: ... [Provide JSON Schema]"
4. Execute the network request using the `FireworksApiService`.
5. Map network exceptions, timeouts, or API error codes to `AiReviewStage.Error(message)` to prevent UI crashes.
6. Deserialize the JSON response content to `AiReviewReport` and emit `AiReviewStage.Complete(report)`.

### Task 4.6: Configure Dependency Injection (Dagger Hilt)
In `DataModule.kt`:
1. Provide the `OkHttpClient` with an interceptor adding `Authorization: Bearer <API_KEY>` and the Logging Interceptor.
2. Provide the `Retrofit` instance pointing to the Fireworks.ai base URL (`https://api.fireworks.ai/inference/v1/`).
3. Provide the `FireworksApiService` bean.
4. Modify `RepositoryModule` to bind `AiReviewRepository` to the new `FireworksAiReviewRepository` instead of `FakeAiReviewRepository`.

---

## 5. Execution Guidelines
* Thoroughly verify that your JSON parsing logic is robust.
* Since network requests can fail, provide graceful error handling so that the user receives an informative, non-crashing status message on error.
* Do not make manual API requests inside ViewModels; keep them clean and let the Repository handle the networking and Flow emissions.
