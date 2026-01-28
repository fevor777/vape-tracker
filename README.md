# Smoke Tracker

A simple Android application to track your smoking habits.

## Features

- **Big Smoke Button**: Tap the large button to log each cigarette
- **Time Since Last Smoke**: See how long it's been since your last cigarette (HH:MM format)
- **Daily Counter**: Track how many cigarettes you've smoked today (resets at midnight)
- **Data Persistence**: Your data is saved locally and persists across app restarts

## Requirements

- Android SDK 26+ (Android 8.0 Oreo or higher)
- Android Studio Hedgehog (2023.1.1) or newer

## Building the App

1. Open the project in Android Studio
2. Sync Gradle files
3. Run on an emulator or physical device

### Command Line Build

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material 3** - Latest Material Design components
- **SharedPreferences** - Local data persistence

## Screenshots

The app features:
- A centered smoke button with emoji and text
- A stats card showing time since last smoke and daily count
- Support for both light and dark themes
- Dynamic colors on Android 12+

## License

MIT License
