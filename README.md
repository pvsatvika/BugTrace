# BugTrace — Diagnostic Telemetry & Automated Bug Reproduction System

BugTrace is an end-to-end diagnostic prototype built for the iQOO Hackathon. It captures real physical Android device telemetry and connects it to a web-based **Developer Console** to generate structured bug reproduction reports.

---

## 🏗 System Architecture

```
┌──────────────────────────────┐              Wi-Fi (HTTP / JSON)             ┌────────────────────────────────┐
│   Physical Android Phone     │ ───────────────────────────────────────────> │     BugTrace Windows Backend   │
│   (Real Device Telemetry &   │                                              │    (FastAPI + SQLite Engine)   │
│   Simulated Demo Scenarios)  │ <─────────────────────────────────────────── │     http://0.0.0.0:8000        │
└──────────────────────────────┘              JSON Responses & Reports        └────────────────────────────────┘
               ▲                                                                              │
               │ ADB USB Deployment (deploy.ps1)                                              │ Serves Dashboard
               │                                                                              ▼
┌──────────────────────────────┐                                              ┌────────────────────────────────┐
│      Development Laptop      │                                              │   BugTrace Developer Console   │
│   (Gradle Build & ADB Auto)  │                                              │   http://127.0.0.1:8000/dashboard│
└──────────────────────────────┘                                              └────────────────────────────────┘
```

1. **Android App (`com.bugtrace.app`)**: Runs on the physical Android phone. Collects real device telemetry (battery level/charging status, network type, orientation history, dynamic process CPU load, and user action events).
2. **FastAPI Backend (`backend/app/main.py`)**: Runs on the Windows laptop on port `8000`. Receives telemetry, stores logs in SQLite (`bugtrace.db`), runs multi-signal analysis, and generates reports.
3. **Developer Console (`http://127.0.0.1:8000/dashboard`)**: Web-based developer dashboard displaying captures, timelines, event sequences, and analyzed reproduction conditions.
4. **ADB Deployment Automation (`deploy.ps1`)**: One-step build & auto-install script that compiles the Android debug APK and installs it over USB without losing app state.

---

## 🚀 Quick Start Guide

### 1. Start the Backend Server

```powershell
cd backend
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- Access **Developer Console**: `http://127.0.0.1:8000/dashboard`
- Access **Swagger API Docs**: `http://127.0.0.1:8000/docs`

### 2. Auto-Deploy Android App to Phone

Plug your Android phone into your laptop via USB (with USB Debugging enabled), then run:

```powershell
.\deploy.ps1
```
*or*
```cmd
.\deploy.bat
```

The script will automatically:
1. Compile the latest Android debug APK with Gradle wrapper (`gradlew assembleDebug`).
2. Detect the connected phone via ADB.
3. Perform an in-place update install (`adb install -r`) preserving app data.
4. Launch BugTrace (`com.bugtrace.app/.MainActivity`) automatically.

---

## 📊 Telemetry Analysis & Report Rules

BugTrace evaluates multiple device signals independently without hardcoding false defects:

| Telemetry Signal | Threshold Condition | Result Flagged |
| :--- | :--- | :--- |
| **Battery** | `< 20%` | `LOW BATTERY DETECTED` |
| **Orientation** | `Landscape` | `LANDSCAPE ORIENTATION DETECTED` |
| **Network** | `Weak` or `Offline` | `WEAK NETWORK DETECTED` |
| **CPU Load** | `> 80%` | `HIGH CPU LOAD DETECTED` |
| **Normal Baseline** | All signals nominal | `NO SIGNIFICANT ANOMALY DETECTED` |

### Detection Statuses
- **`ANOMALY DETECTED`**: Triggered when one or more telemetry thresholds are breached.
- **`NO SIGNIFICANT ANOMALY DETECTED`**: Triggered when all device telemetry remains within nominal bounds (e.g. 44% Battery, Wi-Fi, Portrait, 12% CPU).

### Data Source Classification
- **`REAL DEVICE TELEMETRY`**: Generated from active physical device capture on the phone.
- **`SIMULATED DEMO DATA`**: Generated from pre-set demo scenarios in Demo Mode.

---

## 🧪 Testing Suite

Run the backend unit and API test suite:

```powershell
cd backend
python -m unittest discover -s tests -p "test_*.py"
```

The test suite validates:
- Real-device normal telemetry (`NO SIGNIFICANT ANOMALY DETECTED`)
- High CPU condition (`> 80%`)
- Low battery condition (`< 20%`)
- Weak / offline network
- Landscape orientation transitions
- Multiple simultaneous anomaly conditions
- Demo scenario simulation badges
- Missing / malformed telemetry input handling
- FastAPI endpoints (`/logs`, `/analyze`, `/report`, `/api/demo/trigger`)

---

## 📁 Repository Structure

```text
BugTrace/
├── android/                   # Kotlin Jetpack Compose Android Application
│   ├── app/src/main/java/     # App source (TelemetryCollector, ApiClient, Models, UI)
│   └── build.gradle.kts       # Gradle configuration
├── backend/                   # FastAPI Backend & Developer Console
│   ├── app/
│   │   ├── main.py            # Route handlers & API definitions
│   │   ├── analyzer.py        # Telemetry rule engine & report generator
│   │   ├── models.py          # Pydantic schemas
│   │   ├── storage.py         # SQLite persistence repository
│   │   └── static/            # Developer Console UI (dashboard.html)
│   └── tests/                 # Unit & API test suite
├── deploy.ps1                 # PowerShell automatic ADB build & deploy script
├── deploy.bat                 # CMD batch wrapper for deploy.ps1
└── README.md                  # System documentation
```
