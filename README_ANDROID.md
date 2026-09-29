# Moontir — Native Android (Kotlin & Jetpack Compose)

Moontir is a modern on-demand 4-wheeled vehicle service and detailing mobile app rebuilt from the ground up using **Kotlin**, **Jetpack Compose (Material 3)**, and Android Studio modern architecture guidelines.

---

## 🚀 Quick Start in Android Studio

This project is 100% compatible with Android Studio (Ladybug / Koala / Meerkat and newer).

### Method 1: Open Root Directory (Recommended)
1. Open **Android Studio**.
2. Select **File -> Open...** (or click **Open** on the Welcome screen).
3. Select the repository root folder: `Moontir-main`.
4. Android Studio will automatically recognize the Gradle project and trigger a Gradle Sync.
5. Select a device or emulator and press the green **Run (Shift + F10)** button.

### Method 2: Open `android/` Subdirectory
If you prefer opening the `android` folder specifically:
1. Select **File -> Open...** -> `Moontir-main/android`.
2. Android Studio will link to the `:app` module and sync seamlessly.

---

## 🛠 Project Tech Stack & Architecture

- **Language**: Kotlin 2.3.20 (JVM Toolchain 17)
- **UI Framework**: Jetpack Compose with Material 3 (`androidx.compose.material3`)
- **Architecture**: MVVM with unidirectional data flow (`StateFlow`, `ViewModel`, `Coroutines`)
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 with Gson serialization
- **Image Loading**: Coil Compose 2.7.0 (`AsyncImage`)
- **Maps & Geolocation**:
  - Interactive dark-mode OpenStreetMap Leaflet WebView (`LeafletMapView`) with JavaScript two-way bridge for drag-and-drop pin coordinates
  - Google Play Services Location & Nominatim geocoding / reverse-geocoding
- **Storage & State**: `SharedPreferences` session management (`SessionManager`) with reactive StateFlows
- **Design Tokens**: Personality 7 Dark-First Utility DARK theme (#0C0E14 surface, #3B82F6 / #2563EB brand blue, #93C5FD moon glow)

---

## 📱 Features & Screens

1. **Authentication Screen (`AuthScreen`)**:
   - Toggle between Sign In and Create Account
   - Full name, Email, Password inputs with validation
   - Instant language switch chip (EN / ID)
   - Persistent session token storage

2. **Top Bar (`MoontirTopBar`)**:
   - Reactive language switch button (EN ↔ ID)
   - High-contrast Moontir wordmark logo
   - Active orders badge with live count indicator

3. **Bottom Navigation (`MoontirBottomBar`)**:
   - 5 Main tabs: **HOME**, **SERVICES**, **GARAGE**, **ORDERS**, **PROFILE**

4. **Home Screen (`HomeScreen`)**:
   - Personalized greeting ("Good morning, {Name}")
   - Dark-mode luxury car care hero banner with direct "BOOK NOW" action
   - Active service notification card with green pulsing status
   - Category circle filters (Light Service, Detailing, Special Care)
   - Featured popular service packages with Rupiah pricing
   - Recent orders preview

5. **Services Catalog Screen (`ServicesScreen`)**:
   - Real-time search filter across service packages
   - Category filter pills (`ALL`, `LIGHT SERVICE`, `CAR DETAILING`, `MOONTIR SPECIAL CARE`)
   - Itemized package cards with durations, features, and direct booking trigger

6. **Garage Management (`GarageScreen`)**:
   - List of saved vehicles (Make, Model, Year, Body Type badge, License Plate)
   - Add new vehicle modal bottom sheet (`VehicleBottomSheet`)
   - Edit vehicle modal bottom sheet
   - Remove vehicle confirmation dialog (`ConfirmDeleteDialog`)

7. **5-Step Booking Flow (`BookingDialog`)**:
   - Step 1: Select Service Package
   - Step 2: Choose Garage Vehicle (with on-the-fly "Add Vehicle" support)
   - Step 3: Service Address & Interactive Map (GPS "Use my location", Nominatim street/city search, custom dark Leaflet pin-drop)
   - Step 4: Schedule Selection (Horizontal date chips for next 5 days + 4 time slot chips + specialist notes)
   - Step 5: Review & Itemized Quote Breakdown (dynamic vehicle type surcharge e.g. MPV +15%, SUV +25%, Truck +40% + "Pay after service" zero-friction dispatch confirmation)

8. **Orders & Tracking (`OrdersScreen`)**:
   - Complete history of dispatch orders
   - Interactive status pills ("IN SERVICE" / "COMPLETED")
   - "Mark Complete" button
   - "Rate Specialist" 5-star rating sheet (`RatingBottomSheet`)
   - Detailed itemized invoice modal with timeline history (`InvoiceBottomSheet`)

9. **Profile & Settings (`ProfileScreen`)**:
   - User profile avatar and information
   - Edit display name form
   - Appearance mode selector: System / Dark / Light
   - Instant bilingual language selector: English / Bahasa Indonesia
   - Backend Server URL selector (configure between emulator `http://10.0.2.2:8000/` or local network IP `http://192.168.x.x:8000/`)
   - Sign Out action

---

## 🧪 Testing

Run unit tests via command line:
```bash
./gradlew test
```
Or in Android Studio: right-click `app/src/test/java/com/moontir/app/MoontirAppTest.kt` -> **Run 'MoontirAppTest'**.

Tests cover:
- JSON parsing and data model serialization (Services, Vehicles, Orders, Quotes)
- Indonesian Rupiah currency formatting (`formatRupiah`)
- Quote surcharge calculation matching the backend logic
- Bilingual localization dictionary integrity (EN and ID)

---

## 📦 Building the APK

Generate a debug APK:
```bash
./gradlew assembleDebug
```
Output location:
`app/build/outputs/apk/debug/app-debug.apk`
