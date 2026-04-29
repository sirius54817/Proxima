# Proxima

Proxima is a comprehensive academic companion app designed to help students manage their educational life efficiently. Built with modern Android technologies, it provides a seamless experience for tracking attendance, managing study materials, and organizing academic schedules.

## Key Features

### 📅 Attendance Tracking
- Keep track of class attendance for all your subjects.
- Set attendance goals and visualize your progress with intuitive percentage indicators.
- Quick-action buttons to mark yourself present, absent, or on-duty directly from the home screen.

### 📚 Study Management
- **Notes**: Organize your thoughts with a built-in note-taking system. Supports standard notes and checklists with pinning functionality.
- **Study Materials**: Store and categorize your academic documents (PDFs, PPTs, etc.) by subject for easy access.
- **Weekly Goals**: Set study hour goals for the week and track your progress with a dynamic progress bar and streak counter.

### ⏲️ Focus Mode
- Dedicated study timer to help you stay concentrated.
- Track your study sessions and review your history to improve productivity.

### 🗓️ Timetable & Calendar
- Manage your daily class schedule with a clear, time-slotted timetable.
- **Google Calendar Sync**: Seamlessly sync your academic timetable to and from Google Calendar to keep all your events in one place.

### 🔐 Security & Privacy
- **App Lock**: Protect your academic data with a secure PIN.
- **Biometric Unlock**: Use your device's fingerprint or face recognition for quick and secure access.
- **Encrypted Storage**: Sensitive data and settings are stored using EncryptedSharedPreferences for maximum security.

### ☁️ Cloud Backup
- Securely backup your app data to your personal Google Drive (App Data folder).
- Restore your data across devices or after a fresh install.
- Automatic nightly backups to ensure you never lose your progress.

### 🎨 Modern UI/UX
- Built entirely with **Jetpack Compose**.
- Supports **Material 3** and **Material You** dynamic coloring.
- Fully functional **Dark Mode** support.

## Technologies Used

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Database**: Room persistence library
- **Dependency Injection**: Manual Service Locator / Hilt (internal components)
- **Background Tasks**: WorkManager
- **Security**: Jetpack Security (Crypto) & Biometric API
- **Networking**: OkHttp
- **Cloud Services**: Google Drive API & Google Sign-In
- **Animations**: Lottie & Compose Animations

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on your Android device (Minimum SDK: 26).

---

*Proxima - Streamlining your academic journey.*
