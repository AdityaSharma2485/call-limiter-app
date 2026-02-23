# 📱 Call Limiter

An Android application built to monitor and limit outgoing phone calls — designed to help manage excessive calling behavior for health and wellbeing reasons.

> **Built in 3 days** as a personal project to help a family member with health concerns who was over-calling people.

---

## ✨ Features

- **3-Tier Call Control System**
  - ✅ **Allow** — Calls within the configured limit go through normally
  - 🚫 **Block** — Calls exceeding the limit are blocked with a toast notification
  - 🔀 **Redirect** — After repeated blocked attempts, calls are automatically redirected to a designated helper number

- **Per-Contact Rules** — Set individual call limits for specific contacts
- **Full Dialer Replacement** — Custom dialer with call history, contacts, and settings
- **In-Call UI** — Custom in-call screen with mute, speaker, keypad, and hold controls
- **Real-Time Interception** — Intercepts outgoing calls via `CallScreeningService` and `BroadcastReceiver`
- **Foreground Service** — Persistent monitoring that runs reliably in the background
- **Call Logging** — Tracks all call attempts (allowed, blocked, redirected) with Room DB

---

## 🏗️ Architecture

```
com.example.calllimiter/
├── MainActivity.kt                  # App entry point, permissions, navigation
├── InCallActivity.kt                # Custom in-call UI (Jetpack Compose)
├── domain/
│   └── CallLimiterUseCase.kt        # Core business logic (allow/block/redirect)
├── service/
│   ├── InCallService.kt             # Android InCallService for active call management
│   ├── MyCallScreeningService.kt    # Call screening for incoming/outgoing filtering
│   ├── OutgoingCallReceiver.kt      # BroadcastReceiver for outgoing call interception
│   ├── CallLimiterService.kt        # Foreground service for persistent monitoring
│   ├── CallManager.kt               # Singleton call state management
│   └── CallProcessManager.kt        # Anti-duplicate call processing cache
├── ui/
│   ├── viewmodels/
│   │   ├── DialerViewModel.kt       # Dialer screen logic
│   │   ├── CallHistoryViewModel.kt  # Call history with re-dial support
│   │   └── ContactsViewModel.kt     # Contact sync and management
│   ├── composables/                  # Reusable Jetpack Compose components
│   ├── navigation/                   # Navigation graph
│   └── theme/                        # Material Design 3 theming
├── data/
│   ├── AppDao.kt                    # Room DAO for call logs & contact rules
│   └── SettingsRepository.kt        # App settings (redirect number, limits)
└── di/                               # Dagger Hilt dependency injection modules
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Kotlin** | Primary language |
| **Jetpack Compose** | Modern declarative UI |
| **Material Design 3** | UI components and theming |
| **Dagger Hilt** | Dependency injection |
| **Room Database** | Local persistence for call logs and rules |
| **Android Telecom APIs** | `CallScreeningService`, `InCallService`, `BroadcastReceiver` |
| **Coroutines + Flow** | Asynchronous operations and reactive state |
| **MVVM Architecture** | Clean separation of concerns |

---

## 📋 Permissions Required

- `READ_CONTACTS` — Access contact names
- `READ_PHONE_STATE` — Monitor call state
- `CALL_PHONE` — Place and redirect calls
- `READ_CALL_LOG` — Access call history
- Call Screening Role — Required for outgoing call interception

---

## 💡 How It Works

1. The app registers as the device's **call screening service** and **default dialer**
2. When an outgoing call is placed, `OutgoingCallReceiver` intercepts it
3. `CallLimiterUseCase` checks the number against **per-contact rules** in Room DB
4. Based on the attempt count:
   - **Within limit** → Call proceeds normally
   - **Over limit (attempts 2-3)** → Call is blocked, user sees a toast message
   - **Repeated attempts (4+)** → Call is redirected to the configured helper number
5. All attempts are logged for review in the call history screen

---

## 🤝 Why I Built This

My father had health concerns, and I noticed he was over-calling people frequently. Instead of just telling him to stop, I built a solution — an app that gently limits calls and redirects excessive attempts to a family member who can help.

**Built in 3 days.** Sometimes the best projects come from personal need. ❤️

---
