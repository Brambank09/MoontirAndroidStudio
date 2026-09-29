# Moontir

Moontir is an on-demand vehicle service and detailing platform. Atau bahasa kurang kerennya "mekanik panggilan".

## Architecture

- **Android App (`app/` & `android/`)**: Native Android mobile application rebuilt using **Kotlin**, **Jetpack Compose (Material 3)**, Retrofit, Coroutines, StateFlow, and OpenStreetMap Leaflet integration. Fully compatible with Android Studio.
- **Backend (`backend/`)**: FastAPI Python server with MongoDB, JWT authentication, and vehicle service quoting.
- **Documentation**: See [README_ANDROID.md](file:///d:/koding_projek/Moontir-main/README_ANDROID.md) for Android development and setup instructions.

## Opening in Android Studio
Open the root directory `Moontir-main` directly in Android Studio. Gradle will sync automatically and the app can be run via the standard Run configuration.
