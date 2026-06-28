# GlucoLog - Intelligent Patient Management & Clinical Tracker

**GlucoLog** is a comprehensive Android application built with Jetpack Compose and Kotlin designed for real-time tracking, analytical reporting, and secure cloud synchronization of glucose readings, insulin doses, blood pressure metrics, and medical reminders.

---

## 📱 App Specifications & Overview

- **Application ID / Package Name**: `com.glucolog.sunuoy`
- **Target SDK**: Android 15 (SDK 36)
- **Min SDK**: Android 7.0 (API Level 24)
- **Architecture**: MVVM with Jetpack Compose, Kotlin Coroutines, StateFlow, Room DB, Retrofit, and Firebase.

---

## ✨ Key Features

1. **Glucose Monitoring & Trend Analytics**:
   - Record fasting, pre-meal, and post-meal glucose readings with instant target range checks (e.g., 80 - 140 mg/dL).
   - Graphical trend visualization and meal context filtering.

2. **Insulin Delivery & Cartridge Tracking**:
   - Log rapid-acting and long-acting insulin administration.
   - Dynamic pump cartridge capacity & remaining level computation.

3. **Vitals Logging (Blood Pressure & Pulse)**:
   - Track systolic, diastolic, and pulse measurements with clinical categorization.

4. **Reminders & Schedule Management**:
   - Flexible daily or day-of-week reminders for blood sugar checks and insulin doses.

5. **☁️ Cloud Synchronization & Backup**:
   - **Firebase Integration**: Real-time sync with Firebase Authentication and Cloud Firestore for multi-device cold recovery.
   - **Google Drive Backup**: Direct JSON cloud backup and restore capability via Google Drive API.
   - **REST Backend Sync**: Standardized JSON clinical payload transmission with REST endpoints.

---

## 🛠️ Tech Stack & Dependencies

- **UI Framework**: Android Jetpack Compose with Material 3 Design
- **Local Storage**: Room Persistence Library & Encrypted SharedPreferences
- **Cloud BaaS**: Firebase BoM (`firebase-auth`, `firebase-firestore`)
- **Networking & Serialization**: Retrofit 2, OkHttp 4, Moshi
- **Asynchronous Processing**: Kotlin Coroutines & Flow

---

## 🚀 Building & Running Locally

### Prerequisites
- [Android Studio Ladybug](https://developer.android.com/studio) or newer
- JDK 17 / Gradle 9.3.1

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/sunuoy/INULIEN-TRACKER.git
   ```
2. Open the project in Android Studio and perform a Gradle Sync.
3. Place your valid `google-services.json` in the `app/` directory (configured for `com.glucolog.sunuoy`).
4. Build and run on an Android Emulator or physical device:
   ```bash
   ./gradlew installDebug
   ```
5. To generate a signed debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🔒 License & Configuration
Configured for secure clinical tracking and patient privacy compliance.
