# SIH Enhanced Scrap & E-Waste Valuation Prototype (Multimodal AI)

[![Android CI](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)](android/)
[![Backend](https://img.shields.io/badge/Backend-FastAPI%20%7C%20Python%203.12-009688?logo=fastapi&logoColor=white)](backend/)
[![Edge AI](https://img.shields.io/badge/Multimodal%20AI-Gemini%20VLM%20%2B%20Spectral%20CV-FF6F00?logo=tensorflow&logoColor=white)](android/app/src/main/assets/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An enterprise-grade, circular economy platform built for the **Smart India Hackathon (SIH)**. The system connects informal waste pickers, kabadiwalas, and scrap aggregators directly with licensed **CPCB/SPCB** recyclers through **Zero-Shot Multimodal Vision Condition & Hazard Assessment (Option B)**, vernacular voice interactions (Hindi & Marathi), BLE hardware scale bridging, offline-first anti-fraud synchronization, and an emergency ERSS 112 duress alert disguised as a standard calculator.

---

## 📲 Download & Sideload Mobile Application

You can install and run the native Android application directly on any Android smartphone without building from source code:

* 🚀 **Direct APK Download (Root):** [**`app-debug.apk` (Direct Download)**](https://github.com/Elephantpanda6/sih-enhanced-prototype/raw/main/app-debug.apk)
* 📦 **Mirror / Releases:** [**`releases/app-debug.apk`**](https://github.com/Elephantpanda6/sih-enhanced-prototype/raw/main/releases/app-debug.apk)
* **Target Platforms:** Android 8.0 (API 26) through Android 14+ (API 34). Optimized for flagships (RedMagic 11 Pro 24GB RAM) with adaptive throttling fallback for ultra-budget **Android Go** devices (<60MB active RAM footprint).


---

### 📋 Step-by-Step Android Sideloading Guide

Follow these simple steps to install the APK on an Android device:

#### Step 1: Download the APK
1. Open Chrome or any browser on your Android mobile device.
2. Navigate to this repository or tap [**Download app-debug.apk**](https://raw.githubusercontent.com/Elephantpanda6/sih-fullstack-v2/main/app-debug.apk).
3. The browser will notify you that the file may be harmful; tap **"Download anyway"**.

#### Step 2: Enable "Install Unknown Apps"
1. Once the download finishes, tap the notification or open your device's **Files** / **Downloads** app.
2. Tap on `app-debug.apk`.
3. If prompted by Android security:
   * Tap **Settings** in the dialog.
   * Toggle on **"Allow from this source"** for Chrome or Files.
   * Tap the Back button to return to the installer.

#### Step 3: Install & Bypass Play Protect
1. Tap **Install** on the installation prompt.
2. Because this is an educational/hackathon debug build signed with a development key, **Google Play Protect** may show a warning:
   * Tap **"More details"** (or dropdown arrow).
   * Tap **"Install anyway"**.
3. Once completed, tap **Open**.

#### Step 4: Grant Permissions
On first launch, grant the necessary hardware access when prompted:
* **Camera Permission:** Required for on-device real-time scrap classification, edge-density analysis, and rust/oxidation scoring.
* **Location & Nearby Devices (Bluetooth):** Required to discover and connect to digital Bluetooth (BLE) smart weighing scales.
* **Microphone / Audio Recording:** Required for zero-literacy vernacular voice commands in Hindi and Marathi.

#### Step 5: Test the Application Features
* 🎙️ **Voice Screen:** Speak naturally in Hindi or Marathi (e.g., *"पाच किलो तांबे"* or *"१० किलो लोहा"*). The app extracts quantity, commodity category, and automatically computes estimated market value.
* 📷 **Camera AI Screen:** Point your camera at scrap metal, plastic, or e-waste. The edge TensorFlow Lite model classifies the material in real-time (1 frame every 1.5 seconds) to avoid CPU overheating on budget devices.
* ⚖️ **Weighing Scale:** Automatically pairs with BLE load cells to stream tare and net weight directly into transactions.
* 🗺️ **Recycler Locator:** Discovers nearest CPCB/SPCB certified recyclers with live distance calculation (Haversine formula).
* 🛡️ **Secret Duress Mode (Disguised Calculator):** Open the **Calculator** screen. In case of coercion or theft, enter the secret distress code `9999=` — the screen appears as a normal calculator, but silently dispatches emergency ERSS 112 geolocation alerts to the backend.

---

## 🏗️ Repository Structure

```
sih-fullstack-v2/
├── app-debug.apk                      # Sideloadable Android Debug APK (Direct Download)
├── releases/
│   └── app-debug.apk                  # Mirror release APK
├── android/                           # Native Android Mobile App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── assets/models/     # MobileNetV3 TFLite scrap model & labels
│   │   │   │   ├── java/com/example/sihscrap/
│   │   │   │   │   ├── ai/            # Throttled CameraX analyzer & TFLite classifier
│   │   │   │   │   ├── api/           # Retrofit REST client (BackendApi)
│   │   │   │   │   ├── data/          # Room DB (Transactions, SyncQueue, MaterialCatalog)
│   │   │   │   │   │   ├── local/     # Room Entities & DAOs
│   │   │   │   │   │   ├── security/  # SHA-256 Anti-Double-Spend engine
│   │   │   │   │   │   └── sync/      # Compact payload serializer (<230B) & 2G SyncWorker
│   │   │   │   │   ├── hardware/      # BLE scale GATT service & haptic feedback
│   │   │   │   │   ├── ui/screens/    # Jetpack Compose UI (Dashboard, Camera, Valuation, Voice, Locator, Duress)
│   │   │   │   │   └── voice/         # Vernacular voice normalization (Hindi/Marathi)
│   │   │   │   └── res/               # Vector drawables, themes, strings, XML configs
│   │   │   └── test/                  # Unit tests (Anti-Double Spend, Compact Serialization, Voice, BleScale)
│   │   └── build.gradle.kts           # App Gradle dependencies & SDK config (minSdk 24, targetSdk 34)
│   ├── gradle/                        # Gradle wrapper files
│   ├── build.gradle.kts               # Root Gradle script
│   ├── settings.gradle.kts            # Project settings
│   └── README.md                      # Detailed Android architecture guide
│
├── backend/                           # Enterprise FastAPI Backend & AI Pipeline
│   ├── app/
│   │   ├── api/                       # API routes (Auth, Rates, Recyclers, Transactions, Duress SOS, Vision, Voice)
│   │   ├── models/                    # SQLAlchemy models (Users, Rates, Recyclers, Transactions)
│   │   ├── schemas/                   # Pydantic v2 validation models
│   │   ├── services/                  # Business logic (Geo Haversine, Dynamic Pricing, Voice, Spectrometry)
│   │   ├── config.py                  # Production configuration & Firebase Admin SDK
│   │   ├── database.py                # Database connection pool
│   │   └── main.py                    # FastAPI application entrypoint
│   ├── model_training/                # MobileNetV3 model fine-tuning & TFLite quantization
│   ├── tests/                         # Pytest test suite (100% automated coverage)
│   ├── Dockerfile                     # Container deployment image
│   ├── docker-compose.yml             # Fullstack container composition
│   ├── requirements.txt               # Python production dependencies
│   ├── seed_data.py                   # CPCB recyclers & live scrap mandi rate database seeder
│   └── run.py                         # High-performance Uvicorn server launcher
│
├── web/                               # Next.js 15 Web Portal & Recycler Dashboard
│   ├── src/app/                       # App Router (Dashboard, Rates, Recyclers, Calculator)
│   ├── public/                        # Static assets & SVG icons
│   └── package.json                   # Web dependencies
│
├── sih_conversation_summary.md       # Architecture specifications & design decisions
└── README.md                          # Master documentation & sideloading guide
```

---

## ⚡ Quick Start: Running Components Locally

### 1. Backend (FastAPI + Python 3.12)

```powershell
# Navigate to backend directory
cd backend

# Create and activate virtual environment
python -m venv .venv
.\.venv\Scripts\Activate.ps1

# Install dependencies
pip install -r requirements.txt

# Seed the database with official CPCB recyclers and live mandi commodity rates
python seed_data.py

# Run the backend server
python run.py
```
* **API Documentation:** Accessible at `http://localhost:8000/docs` (Swagger UI).
* **Automated Tests:** Run `pytest` inside the `backend` folder.

---

### 2. Android App (Kotlin + Jetpack Compose)

```powershell
# Navigate to android directory
cd android

# Run all automated tests (Anti-Double Spend, Voice Engine, BleScale, Serialization)
.\gradlew test

# Assemble the release/debug APK
.\gradlew assembleDebug
```
* The freshly compiled APK will be at `android/app/build/outputs/apk/debug/app-debug.apk`.
* To deploy directly to a connected physical phone over USB:
  ```powershell
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

---

### 3. Web Dashboard (Next.js 15)

```powershell
# Navigate to web directory
cd web

# Install dependencies
npm install

# Launch local development server
npm run dev
```
* Dashboard will be live at `http://localhost:3000`.

---

## 🛡️ Core Innovation & Architecture

### 1. Anti-Double-Spend Offline Cryptographic Engine
Informal waste collectors often operate in remote areas or basements with zero cellular connectivity.
* Transactions are generated offline and signed with `SHA-256(prev_hash + timestamp + collector_id + weight + category_id)`.
* Every transaction must strictly reference the previous unspent transaction hash, forming an immutable local tamper-proof chain.
* When re-establishing connectivity, `CompactPayloadSerializer` encodes records into binary payloads under **230 bytes**, ensuring instant transmission over fragile **2G/EDGE** cellular connections without packet drops.

### 2. Zero-Literacy Vernacular Voice Engine
* Custom phoneme and digit normalizers parse vernacular Hindi (`"लोहा"`, `"तांबा"`, `"पीतल"`, `"रद्दी"`) and Marathi (`"लोखंड"`, `"तांबे"`, `"भंगार"`).
* Extracts spoken quantities, converts them into standard SI kilogram units, and matches them to official CPCB commodity classifications.

### 3. Throttled Edge AI Vision Pipeline
* Low-end devices running Android Go overheat when running continuous CameraX frame analyzers.
* Our `ThrottledImageAnalyzer` drops intermediate frames, processing exactly 1 frame every 1,500ms using a quantized `MobileNetV3` TFLite model (~1.1MB size).
* Evaluates edge rust density via HSV channel inspection and color spectrometry without cloud round-trips.

---

## 👥 Authors & Acknowledgments
Developed for the **Smart India Hackathon (SIH)** — Empowering India's informal waste workforce with transparent, AI-backed valuation and ethical recycling pipelines.
