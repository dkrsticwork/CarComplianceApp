# Car Ownership Compliance Assistant
### Android MVP — Kotlin + Jetpack Compose

---

## What this app does

Answers: **"What does this car owner need to do next, and when?"**

- Generates a personalized legal + maintenance obligation timeline per car
- Uses AI (your own API key) to match rules to your country, car, and documents
- Sends push notifications 30 / 7 / 1 day before each deadline
- Supports multiple cars ("Garage")
- Trust-first: no ads, no paywalls, every task has a "why" explanation

---

## Quick setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35 (auto-installed by Android Studio)
- Java 17 (bundled with Android Studio)
- A physical Android device or emulator running Android 8.0+ (API 26+)

### 1. Open in Android Studio
```
File → Open → select the CarComplianceApp folder
```

### 2. Let Gradle sync
Android Studio will download all dependencies automatically (~2–3 min first run).

### 3. Run
Click **Run ▶** or press `Shift+F10`. Select your device/emulator.

### 4. Get an AI API key (one of these)
| Provider | Key prefix | Where to get |
|---|---|---|
| OpenAI | `sk-...` | platform.openai.com |
| Anthropic | `sk-ant-...` | console.anthropic.com |
| Google Gemini | `AIza...` | aistudio.google.com |
| Mistral | `mis...` | console.mistral.ai |
| Cohere | (32 chars) | dashboard.cohere.com |

Paste your key in the app on first launch — it's auto-detected and stored only on your device.

---

## Project structure

```
app/src/main/java/com/carcomplianceapp/
├── data/
│   ├── local/
│   │   ├── CarComplianceDatabase.kt    Room database
│   │   ├── PreferencesManager.kt       DataStore (API key, settings)
│   │   ├── dao/Daos.kt                 Room DAOs
│   │   └── entity/Entities.kt          DB entities
│   ├── remote/
│   │   └── AiApiService.kt             Unified AI caller (5 providers)
│   └── repository/
│       ├── Repositories.kt             Data access layer
│       └── Mappers.kt                  Entity ↔ domain converters
├── domain/model/
│   └── Models.kt                       Car, ComplianceTask, ApiKeyConfig…
├── di/
│   └── AppModules.kt                   Hilt DI modules
├── ui/
│   ├── NavGraph.kt                     Navigation
│   ├── components/Components.kt        Reusable Compose components
│   ├── theme/                          Material3 theme, colors, typography
│   ├── screens/
│   │   ├── onboarding/WelcomeScreen.kt
│   │   ├── apikey/ApiKeyScreen.kt
│   │   ├── addcar/AddCarScreen.kt
│   │   └── main/
│   │       ├── MainScreen.kt           Bottom nav host
│   │       ├── timeline/TimelineTab.kt
│   │       ├── actions/ActionsTab.kt
│   │       ├── garage/GarageTab.kt
│   │       └── settings/SettingsTab.kt
│   └── viewmodel/ViewModels.kt         All ViewModels
├── worker/
│   └── NotificationWorker.kt           WorkManager + BootReceiver
└── MainActivity.kt                     Entry point
```

---

## Architecture

```
UI (Compose) → ViewModel → Repository → Room DB (local)
                                      → AI API Service (remote)
                                      → DataStore (preferences)
```

**Clean Architecture layers:**
- **Domain** — pure Kotlin models, no Android dependencies
- **Data** — Room, Retrofit/OkHttp, DataStore
- **UI** — Jetpack Compose, ViewModels, Hilt navigation

---

## AI API key error handling (trust-critical)

The app distinguishes and explains every failure mode:

| HTTP code / error | What the app shows |
|---|---|
| 401 | "Key invalid or revoked — check your provider dashboard" |
| 402 | "Insufficient funds — add billing credits to your account" |
| 403 | "Key expired or lacks permission" |
| 429 | "Rate limited — wait a few minutes" |
| Network error | "No internet connection — saved tasks still available" |
| Parse error | "AI responded but result couldn't be parsed — try refreshing" |

**In all cases:** saved tasks remain visible and usable. The error never blocks the app.

---

## Notifications

WorkManager runs a daily check. For each task due within 30 / 7 / 1 days, a notification is sent (respecting user preferences in Settings). The device boot receiver reschedules on restart.

---

## Multi-car support

Tap **Garage** → **+** to add additional cars. Each car has its own task list. The active car's tasks are shown in Timeline and Actions.

---

## Non-negotiable product rules (enforced in code)

- ✅ No ads, no paywalls, no monetization
- ✅ No telemetry — all data local only
- ✅ Uncertain dates shown as windows (e.g. "May–June 2025")
- ✅ Every task has a visible "Why this is shown" explanation
- ✅ Users can edit/override any task
- ✅ API key errors never block access to saved data

---

## Extending the app

**Add a country's legal rules as a template:**
Edit the AI prompt in `AiApiService.kt → buildPrompt()`. You can add hardcoded country-specific context for faster/more reliable responses.

**Add a new AI provider:**
Add to `AiProvider` enum in `Models.kt`, implement the call in `AiApiService.kt`, and update `detectProvider()`.

**Add document OCR/analysis:**
In `AddCarScreen.kt`, after the user picks a file, extract text and pass it as `documentSummaries` to `generateAndSaveTasks()`.

---

## Build release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

Sign with your keystore for Play Store distribution.
