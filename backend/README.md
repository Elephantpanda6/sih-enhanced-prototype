# SIH Smart Scrap & E-Waste Dynamic Valuation System

A production-grade enterprise platform engineered for the circular economy, informal scrap collection networks (Kabadiwala systems), and government-regulated e-waste/battery recycling hubs under **CPCB** and **SPCB** mandates.

---

## 📱 Native Android Mobile App Download & Sideloading

The native Android application built with Jetpack Compose, CameraX, and TensorFlow Lite is available for immediate mobile sideloading:

* **Branch:** [`feature/native-android-app`](https://github.com/Elephantpanda6/sih-scrap-backend/tree/feature/native-android-app)
* **Direct APK Download:** [Download `app-debug.apk`](https://raw.githubusercontent.com/Elephantpanda6/sih-scrap-backend/feature/native-android-app/app-debug.apk)
* **Features:** Offline Room DB with Anti-Double-Spend (`SHA-256`), Throttled CameraX ML Scanner (1 frame/1.5s for Android Go), BLE Scale integration, Vernacular Voice Assistant (Hindi & Marathi), and Secret ERSS 112 Duress SOS Calculator.

---

## Architecture & Core Modules

```
sih-scrap-backend/
├── app/
│   ├── api/
│   │   ├── auth.py          # Phone + PIN authentication & JWT role management
│   │   ├── pricing.py       # Multi-tier dynamic valuation with precious metal recovery
│   │   ├── rates.py         # Daily market mandi commodity spot rates
│   │   ├── recyclers.py     # CPCB geospatial nearest-neighbour locator
│   │   ├── transactions.py  # Pickup orders & collection lifecycle
│   │   ├── vision.py        # Offline scrap image feature extraction & grading
│   │   └── voice.py         # Exclusive Hindi & Marathi voice-to-command recognition
│   ├── models/
│   │   ├── category.py      # Two-tier taxonomy (Category & SubCategory) with metallurgy
│   │   ├── recycler.py      # CPCB/SPCB authorized recycler registry
│   │   ├── transaction.py   # Scrap transactions & pickup schedules
│   │   └── user.py          # User identities & role permissions (dealer, buyer, admin)
│   ├── schemas/             # Strict Pydantic v2 schemas (TypeScript / Flutter ready)
│   ├── services/
│   │   ├── auth_service.py  # Bcrypt password hashing & JWT token issuing
│   │   ├── geo_service.py   # Haversine distance, travel times & navigation links
│   │   ├── pricing_service.py # Dynamic valuation, oxidation, precious metals & CO2 metrics
│   │   ├── vision_service.py  # Offline CPU MobileNet-V3 spectrometry & edge analysis
│   │   └── voice_service.py   # Vernacular slot filling, audio decoding & PCM TTS
│   ├── config.py            # Production settings & structured logging
│   ├── database.py          # SQLAlchemy 2.0 connection pool
│   └── main.py              # Application entrypoint with standardized exception handlers
├── tests/                   # Comprehensive automated test suite (Pytest + In-Memory DB)
│   ├── conftest.py          # Isolated SQLite database fixtures & seeded test records
│   ├── test_auth.py         # Registration, login & buyer privilege enforcement
│   ├── test_pricing_hierarchy.py # Rust deductions, gold/palladium yields, floor payout
│   ├── test_recyclers_geo.py # Haversine locator, lat/lon params, CPCB registry
│   ├── test_vision_offline.py# Rust analysis, PCB classification, edge density
│   └── test_voice_marathi_hindi.py # Numbers, dialects, intent, audio decoding, TTS
├── requirements.txt         # Production dependencies
├── seed_data.py             # Complete database seeder with authentic Indian entities
├── test_endpoints.py        # End-to-end integration test script
└── run.py                   # High-performance Uvicorn server launcher with preflight checks
```

---

## 1. Exclusive Hindi & Marathi Voice-to-Command Engine

Designed specifically for low-literacy informal waste collectors and kabadiwalas across Maharashtra and Northern/Central India.

### Vernacular Capabilities
* **Full Spoken Number Parsing**:
  * **Marathi**: "paav" (0.25), "ardha" (0.5), "paun" (0.75), "sava" (1.25), "dedh" (1.5), "don" (2), "adhich" (2.5), "teen" (3), "char" (4), "paach" (5), "saha" (6), "saat" (7), "aath" (8), "nau" (9), "daha" (10), "pandhra" (15), "vis" (20), "panchvis" (25), "tis" (30), "chalis" (40), "pannas" (50), "shambhar" (100).
  * **Hindi**: "paon" (0.25), "aadha" (0.5), "dedh" (1.5), "do" (2), "dhai" (2.5), "teen" (3), "chaar" (4), "paanch" (5), "das" (10), "pandrah" (15), "bees" (20), "pachees" (25), "tees" (30), "pachas" (50), "sau" (100).
  * Native support for both **Devanagari script** (e.g., *"दोन किलो तांबे आणि पाच किलो लोखंड भाव सांगा"*) and **Latin phonetic transliteration** (*"don kilo tamba aani paach kilo lokhand bhav sanga"*).
* **Regional Slang & Trade Term Mapping**:
  * **Iron**: Loha, Lokhand, Lohand, Sariya, Kadak lokhand, Bhaari loha.
  * **Copper**: Taamba, Taambe, Chokha tamba, Bare bright, Tambi taar.
  * **Brass**: Peetal, Pital, Pitali bhandi, Honey brass.
  * **Scrap / Trade**: Bhangar, Kabaad, Mandi.
  * **Sheet / Containers**: Patra, Tin patra, Chadar, Dabba, Khoka.
  * **Paper / Cardboard**: Raddi, Raddi gatta, Gatta, Carton, Akhbar, Varthamanpatra.
  * **Bottles**: Botal, Batli, Batlya, Pet botal.
* **Functional Command Intent Detection**:
  * `price_inquiry`: Spoken triggers like *"bhav sanga"*, *"mulya sanga"*, *"dar sanga"*, *"bhav batao"*, *"keemat batao"*, *"rate batao"*.
  * `pickup_request`: Spoken triggers like *"pickup pathva"*, *"gadi pathva"*, *"ghyaun ja"*, *"pickup bhejo"*, *"gaadi bhejo"*, *"maal uthao"*.
  * `weight_query`: Spoken triggers like *"wajan dakhva"*, *"wajan sanga"*, *"wajan dikhao"*, *"kitna kilo hai"*.
* **Offline Audio Decoding Pipeline (`POST /api/v1/voice/decode-audio`)**:
  * Decodes audio bytes (WAV, MP3, OGG) completely on CPU without cloud APIs.
  * Extracts zero-crossing rates, duration envelopes, and phoneme spectral profiles.
* **Fluent Vernacular TTS Confirmation Speech**:
  * Every response includes grammatically fluent confirmation text in both Hindi and Marathi.
  * Generates playable **16kHz 16-bit PCM WAV audio data URI** (`data:audio/wav;base64,...`) and streamable audio endpoint (`GET /api/v1/voice/tts`).

---

## 2. Hierarchical Scrap Taxonomy (Category & Sub-Category)

Two-tier circular economy taxonomy with physical and metallurgical properties:

| Category | Sub-Category | Density (kg/m³) | Spot Rate (₹/kg) | Rust / Oxidation Factor | Precious Metal Yields | GST HSN |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Non-Ferrous** | Copper Bare Bright | 8,960 | ₹695 | 0.05 | - | 740400 |
| **Non-Ferrous** | Copper Armature (Motor Winding) | 8,700 | ₹635 | 0.08 | - | 740400 |
| **Non-Ferrous** | Brass Honey (Yellow Scrap) | 8,500 | ₹465 | 0.05 | - | 740400 |
| **Non-Ferrous** | Aluminum Extrusion | 2,700 | ₹175 | 0.03 | - | 760200 |
| **Non-Ferrous** | Aluminum Castings (Engine Blocks) | 2,680 | ₹142 | 0.06 | - | 760200 |
| **Non-Ferrous** | Aluminum Old Utensils (Bartan) | 2,650 | ₹148 | 0.05 | - | 760200 |
| **Ferrous** | Heavy Melting Steel (HMS / Sariya) | 7,850 | ₹39.50 | 0.20 | - | 720449 |
| **Ferrous** | Light Iron / Sheet Metal (Patra) | 7,600 | ₹29.00 | 0.25 | - | 720449 |
| **Ferrous** | Cast Iron (Chulha / Machine Parts) | 7,200 | ₹35.00 | 0.15 | - | 720410 |
| **E-Waste (WEEE)** | High-Grade Server / Telecom PCB | 1,850 | ₹320.00 | 0.02 | Au: 180g/t, Ag: 650g/t, **Pd: 35g/t** | 854911 |
| **E-Waste (WEEE)** | Computer Motherboard | 1,750 | ₹195.00 | 0.04 | Au: 85g/t, Ag: 320g/t, **Pd: 15g/t** | 854911 |
| **E-Waste (WEEE)** | Smartphone / Mobile PCB | 1,600 | ₹380.00 | 0.01 | Au: 220g/t, Ag: 800g/t, **Pd: 45g/t** | 854912 |
| **E-Waste (WEEE)** | CRT & LCD Display Glass | 2,500 | ₹8.50 | 0.00 | Lead / Indium Recovery | 854919 |
| **E-Waste (WEEE)** | Lithium-Ion Cylindrical / Pouch Cells | 2,100 | ₹140.00 | 0.02 | Cobalt / Nickel Yield | 854913 |
| **Battery & Hazmat** | Lead-Acid Inverter / Automotive Battery | 2,400 | ₹98.00 | 0.05 | Secondary Lead Yield | 854810 |
| **Battery & Hazmat** | Lithium-Ion EV / Mobile Battery Packs | 2,100 | ₹140.00 | 0.02 | Lithium Carbonate Yield | 854810 |
| **Plastics** | PET Bottles (Clear Baled) | 350 | ₹27.50 | 0.00 | Food-grade rPET | 391590 |
| **Plastics** | HDPE Rigid Plastic (Drums / Crates) | 950 | ₹34.00 | 0.00 | Blow-molding re-pellet | 391510 |
| **Paper/Cardboard** | Corrugated Cardboard (OCC / Khoka) | 180 | ₹14.50 | 0.00 | Cellulose pulp reuse | 470710 |
| **Paper/Cardboard** | Old Newspaper (ONP / Raddi Akhbar) | 220 | ₹18.00 | 0.00 | De-inked newsprint pulp | 470790 |

---

## 3. CPCB & SPCB Recycler Registry & Geospatial Locator

Complies with **E-Waste (Management) Rules, 2022** and **Battery Waste Management Rules, 2022**.

### Registered Hubs
* **Maharashtra (MPCB)**: Eco Recycling Limited (Ecoreco, Mahape Navi Mumbai), Green Enviro Recycling Hub (Chakan, Pune), E-Incarnation Recycling (Boisar, Palghar), Horizon Metal & E-Recycling (Butibori, Nagpur), Mahalaxmi E-Waste (Ambad MIDC, Nashik).
* **Delhi-NCR (CPCB / DPCC / UPPCB / HSPCB)**: Attero Recycling (Greater Noida), Greeniva Recycler (Ghaziabad), Exigo Recycling (Gurugram), Bharat E-Waste Solutions (Okhla, New Delhi).
* **Gujarat (GPCB)**: E-Coli Waste Management (Changodar, Ahmedabad), Earth Sense Recycle (Panoli, Ankleshwar), Baroda E-Waste Solutions (Makarpura, Vadodara).
* **Karnataka (KSPCB)**: Cerebra Green (Narasapura, Bengaluru), Saahas Zero Waste (Peenya, Bengaluru), E-R3 Solutions (Whitefield, Bengaluru).
* **Tamil Nadu (TNPCB)**: TES-AMM India (Sriperumbudur / Oragadam, Chennai), Tritech Systems (Ambattur, Chennai), Green Era Recyclers (Kurichi, Coimbatore).

### Nearest Recycler Endpoint
`GET /api/v1/recyclers/nearest?lat=...&lon=...&category=...`
*(Also accepts `latitude` and `longitude` aliases)*
* Calculates exact great-circle distance using the **Haversine formula**.
* Computes estimated commercial transit driving times.
* Emits one-touch Google Maps driving direction URLs (`google_maps_directions_url`).
* Generates spoken turn-by-turn guidance in fluent Marathi and Hindi.

---

## 4. Offline CPU Vision Classifier & Spectrometry

Runs 100% locally on CPU using embedded multi-spectral analysis and MobileNet-V3 tensor processing.

* **Surface Rust & Oxidation Color Scoring**: Identifies $Fe_2O_3$ spectral absorption peaks where $R > 1.35 \times B$ and $R > 1.15 \times G$, outputting exact oxidation percentage.
* **Green Solder Mask / PCB Detection**: Detects copper trace density and chrominance contrast on electronic circuit boards.
* **Metallic Specular Reflectance Score**: High-luminance highlight histogram indexing.
* **Material Spatial Edge Density**: Laplacian / Sobel gradient convolution measuring structural complexity.
* **Cleanliness & Contamination Grading**:
  * `Grade A`: Clean, unoxidized bare metal / millberry scrap.
  * `Grade B`: Moderate surface oxidation / light dirt.
  * `Grade C`: Heavy rust, concrete attachment, or composite sludge.
* **Vernacular Segregation Guidance**: Practical field advice in Hindi and Marathi (e.g., separating copper stator coils from cast iron casings to increase net payout).

---

## 5. Enterprise Engineering & Verification

### Running the Server
```powershell
.venv\Scripts\python.exe run.py
```
* **Interactive Swagger UI**: `http://localhost:8000/docs`
* **Alternative ReDoc UI**: `http://localhost:8000/redoc`

### Seeding the Database
```powershell
.venv\Scripts\python.exe seed_data.py
```

### Running the Automated Test Suite
```powershell
.venv\Scripts\pytest
```

### Running End-to-End Pillar Verification
```powershell
.venv\Scripts\python.exe test_endpoints.py
```

---

## API Contract Overview

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/v1/auth/register` | `POST` | Onboards scrap dealer or buyer with phone and PIN |
| `/api/v1/auth/login-json` | `POST` | Authenticates user and returns JWT bearer token |
| `/api/v1/auth/me` | `GET` | Retrieves profile of currently authenticated user |
| `/api/v1/voice/parse` | `POST` | Parses spoken Marathi/Hindi transcripts into valuation |
| `/api/v1/voice/decode-audio` | `POST` | Decodes uploaded audio WAV/MP3/OGG on local CPU |
| `/api/v1/voice/tts` | `GET` | Streams native synthesized WAV audio response |
| `/api/v1/voice/vocabulary` | `GET` | Exports vernacular vocabulary and number systems |
| `/api/v1/rates/categories` | `GET` | Returns two-tier category hierarchy with spot rates |
| `/api/v1/rates/subcategories` | `GET` | Returns flat list of all scrap commodities |
| `/api/v1/pricing/calculate-hierarchical` | `POST` | Dynamic valuation with precious metals & rust discounts |
| `/api/v1/recyclers/nearest` | `GET` | Haversine nearest CPCB/SPCB authorized recyclers |
| `/api/v1/recyclers/` | `GET` | Verified registry filtered by state or regulatory board |
| `/api/v1/vision/analyze-image` | `POST` | Offline scrap classification, rust scoring & grading |
| `/api/v1/transactions/` | `POST` | Creates scrap pickup booking with dealer margin applied |
