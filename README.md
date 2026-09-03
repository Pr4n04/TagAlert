# TagAlert

**Never leave your keys behind.**

TagAlert is a "left behind" alert app for Android that works with BLE trackers like the Ugreen Finder Pro (Google Find Hub). When your tracker goes out of range, you get a notification and vibration alert.

## Why?

Google Find Hub doesn't have a native "left behind" notification (Apple Find My does). TagAlert fills that gap for Android users.

## Features

- **Left-behind detection** — Dual-layer: `CompanionDeviceManager` (OS-level, battery-efficient) + BLE scan fallback
- **Customizable countdown** — 15s to 5min, default 90s (perfect for walking between lecture rooms)
- **Notification + vibration** — High-priority alerts you won't miss
- **Last seen log** — Timestamped history of where your tracker has been
- **Safe zones** — Suppress alerts at known locations (home, university)
- **Battery efficient** — Low-power BLE scanning with OS-managed presence monitoring
- **Boot persistence** — Automatically resumes tracking after reboot
- **Material 3 UI** — Clean, dark-theme-friendly Jetpack Compose interface

## How It Works

### Dual-Layer Detection

```
┌─────────────────────────────────────┐
│  Layer 1: CompanionDeviceManager    │  ← Primary (most battery-efficient)
│  OS watches BLE range automatically │
│  Binds service when device appears  │
│  Unbinds when device disappears     │
├─────────────────────────────────────┤
│  Layer 2: BLE Scan Fallback         │  ← Backup (if CompanionDevice misses)
│  Low-power scan every 30 seconds    │
│  PendingIntent-based for resilience │
└─────────────────────────────────────┘
```

### Detection Flow

1. Tracker enters BLE range → Mark "with you", cancel pending alerts
2. Tracker leaves BLE range → Start countdown (default 90s)
3. Countdown expires while still away → **HIGH-PRIORITY notification + vibration**
4. User taps "I'm Back" or tracker re-appears → Clear alert

## Requirements

- Android 9.0+ (API 28)
- A BLE tracker (Ugreen Finder Pro, or any Google Find Hub tracker)
- Bluetooth enabled
- Location services enabled

## Setup

1. **Install** — Clone and build with Android Studio, or download the APK from Releases
2. **Pair tracker** — Make sure your Ugreen Finder Pro is paired via Google Find Hub
3. **Grant permissions** — Allow Bluetooth, Location (including background), and Notifications
4. **Start tracking** — Toggle "Active Tracking" on the dashboard
5. **Walk away** — TagAlert will alert you when you leave your tracker behind

## Permissions

| Permission | Why |
|-----------|-----|
| `BLUETOOTH_SCAN` | Detect tracker presence |
| `BLUETOOTH_CONNECT` | Connect to tracker |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning |
| `ACCESS_BACKGROUND_LOCATION` | Detect tracker when app is backgrounded |
| `FOREGROUND_SERVICE` | Keep detection running |
| `POST_NOTIFICATIONS` | Show left-behind alerts |
| `RECEIVE_BOOT_COMPLETED` | Resume tracking after reboot |

## Architecture

```
com.tagalert
├── TagAlertApp.kt              # Application + notification channels
├── data/
│   ├── local/
│   │   ├── Daos.kt             # Room DAOs for devices & history
│   │   ├── TagAlertDatabase.kt # Room database
│   │   └── UserPreferences.kt  # DataStore preferences
│   ├── model/
│   │   ├── TrackedDevice.kt    # Device entity
│   │   ├── LocationHistory.kt  # History entity
│   │   └── DevicePresence.kt   # Presence state machine
│   └── repository/
│       └── TagRepository.kt    # Data access layer
├── di/
│   └── AppModule.kt            # Hilt dependency injection
├── service/
│   ├── LeftBehindService.kt    # Core BLE detection + countdown
│   ├── TagCompanionService.kt  # OS-level companion device monitoring
│   ├── BleScanService.kt       # BLE scan fallback
│   ├── BootReceiver.kt         # Auto-restart on boot
│   └── NotificationActionReceiver.kt  # Handle notification actions
├── ui/
│   ├── MainActivity.kt         # Entry point + permissions
│   ├── screens/
│   │   ├── DashboardScreen.kt  # Main status view
│   │   ├── HistoryScreen.kt    # Last seen log
│   │   └── SettingsScreen.kt   # Configuration
│   └── theme/
│       └── Theme.kt            # Material 3 theme
└── util/
    ├── LocationHelper.kt       # Fused location provider
    └── TimeUtils.kt            # Time formatting
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Location**: Google Play Services Fused Location
- **BLE**: Android BluetoothLeScanner + CompanionDeviceManager

## License

MIT License — see [LICENSE](LICENSE) for details.
