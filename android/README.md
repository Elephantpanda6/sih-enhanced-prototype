# SIH Smart Scrap & E-Waste Valuation Mobile Application

An enterprise production native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, and **TensorFlow Lite**. Engineered specifically for informal waste collectors (Kabadiwalas) and licensed CPCB/SPCB recyclers in India, adhering strictly to Android Go memory constraints (< 60MB RAM footprint target) and zero-literacy visual/vernacular accessibility.

---

## 📲 Direct Mobile Download & Sideloading Instructions

You can download and install the pre-compiled Android APK directly on any physical Android smartphone (Android 7.0 / SDK 24 up to Android 14+).

### Option 1: Direct APK Download
* **Primary APK File:** [`app-debug.apk`](app-debug.apk) (Root directory)
* **Releases Directory:** [`releases/app-debug.apk`](releases/app-debug.apk)
* **Direct Raw Download Link:**  
  [Download `app-debug.apk` from GitHub](https://raw.githubusercontent.com/Elephantpanda6/sih-scrap-backend/feature/native-android-app/app-debug.apk)

---

### Step-by-Step Installation & Sideloading Guide

1. **Download APK to Phone:**
   * Open Chrome / browser on your Android mobile device and download [`app-debug.apk`](app-debug.apk).
   * *Alternatively:* Download it on your computer and transfer it to your phone via USB cable, Google Drive, or messaging.
2. **Enable "Install Unknown Apps":**
   * When opening the APK, if Android prompts *"For your security, your phone is not allowed to install unknown apps from this source"*:
   * Tap **Settings** in the popup.
   * Toggle **Allow from this source** to **ON**.
3. **Install the Application:**
   * Return to the installer prompt and tap **Install**.
   * Tap **Open** once installation succeeds.
4. **Grant Runtime Permissions:**
   * **Camera**: Required for the real-time AI scrap grading & oxidation scanner.
   * **Microphone**: Required for Hindi & Marathi vernacular speech queries.
   * **Location**: Required for locating the nearest CPCB/SPCB recycling facilities.
   * **Bluetooth**: Required for wireless digital weight scale synchronization.

---

## 🏗️ Application Architecture & Core Features

### 1. Zero-Literacy Visual & Vernacular Interface
* **High-Contrast Flashcards**: Large 56dp+ touch targets with high-contrast color badges representing recyclable scrap grades.
* **Multilingual Switcher**: Instant one-tap toggle between **Hindi (हिन्दी)**, **Marathi (मराठी)**, and **English**.
* **Audio Guidance Button**: Spoken walkthrough of available screen options for collectors with zero reading literacy.

### 2. Micro-Edge AI & Throttled CameraX Pipeline
* **Throttled Frame Processing**: Operates strictly at **1 frame per 1.5 seconds** downsampled to a 224x224 Y-plane buffer.
* **Deterministic GC Disposal**: Enforces explicit `imageProxy.close()` disposal immediately in a `finally` block to completely eliminate memory spikes and latency on Android Go devices.
* **4-Tier Material Classification & Oxidation Scoring**:
  * **Emerald Green (`#00C853`)**: Clean High-Grade Metals (Copper Bare Bright, Yellow Brass, Aluminium Extrusions).
  * **Amber (`#FFB300`)**: Mixed Scrap (Heavy Steel Sariya, Light Iron Patra, Cardboard OCC, PET Plastic).
  * **Slate (`#607D8B`)**: Inert E-Waste (Server Motherboards, Smartphone PCBs).
  * **Crimson (`#D50000`)**: Hazardous Battery/Lead (Lead-Acid Inverter Batteries, Li-Ion 18650 Cells).
* **HSV Rust Analysis**: Quantifies surface oxidation percentage and applies capped transparent price deductions (max 20%).

### 3. Hardware & Sensor Bridges
* **BLE Weight Scale Integration ([`BleScaleService.kt`](app/src/main/java/com/example/sihscrap/hardware/BleScaleService.kt))**:
  * Bluetooth GATT listener for standard Bluetooth SIG Weight Scale profile (`0x181D`, `0x2A98`).
  * Live Loadcell Simulation Fallback: If no physical scale is connected, generates realistic stabilizing loadcell readings for testing.
* **Vernacular Voice Engine ([`VoiceEngine.kt`](app/src/main/java/com/example/sihscrap/voice/VoiceEngine.kt))**:
  * Number normalization supporting Marathi and Hindi fractions (*paav*, *ardha*, *dedh*, *adhich*, *paun*, *sava*) and Devanagari numerals (*१४.५*).
  * Scrap trade slang recognition (*lokhand*, *bhangar*, *tambha*, *peetal*, *e-kachra*, *raddi*, *batli*).
  * Android Text-To-Speech (TTS) readback in Hindi, Marathi, and English.
* **Anti-Accidental Haptic Lock**:
  * 2-second vibration lock on long-press confirmation to prevent accidental valuation dispatches.

### 4. Offline-First Resilience & Anti-Double-Spend
* **Room Database ([`AppDatabase.java`](app/src/main/java/com/example/sihscrap/data/local/AppDatabase.java))**:
  * `TransactionEntity`: Local ledger with status badges (`PENDING_SYNC`, `SYNCED`, `FAILED`).
  * `MaterialCatalogEntity`: Local spot rate cache with CPCB categories and EPR subsidies (+₹3 to ₹7/kg).
  * `SyncQueueEntity`: FIFO transmission queue with retry counter and exponential backoff.
* **Anti-Double-Spend Engine ([`AntiDoubleSpendEngine.kt`](app/src/main/java/com/example/sihscrap/data/security/AntiDoubleSpendEngine.kt))**:
  * Cryptographic receipt hash: `SHA-256(GPS + Nonce + EpochMillis)`.
* **Low-Bandwidth Sync Engine ([`SyncWorker.kt`](app/src/main/java/com/example/sihscrap/data/sync/SyncWorker.kt))**:
  * Compact JSON payloads (< 230 bytes/submission) optimized for rural 2G/EDGE network drops.

### 5. CPCB/SPCB Recycler Locator
* Interactive **Facility List** and **Map Radar View** displaying authorized recyclers within 100 km.
* Shows operating hours, phone contact, regulatory authorization numbers, and one-tap dispatch booking.

### 6. Emergency Duress SOS Calculator
* Fully functional disguise calculator for everyday math.
* Typing the secret duress code (`911=` or `112=`):
  * Instantly triggers a silent haptic SOS vibration pattern.
  * Silently transmits an emergency ERSS 112 SOS telemetry payload with live GPS coordinates.
  * Switches the display to an innocent **Zero-Balance Decoy Screen** to protect informal waste collectors from robbery or coercion.

---

## 🛠️ Building From Source

### Prerequisites
* JDK 17 (or Android Studio bundled JetBrains Runtime `jbr`)
* Android SDK Platform 34 / 36

### Build Commands
```powershell
# Set JAVA_HOME if not already in environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Run unit tests
.\gradlew.bat test

# Build Debug APK
.\gradlew.bat assembleDebug
```

The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`
