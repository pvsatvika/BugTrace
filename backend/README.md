# Bug Reproduction Logger - Backend

A lightweight, clean FastAPI backend prototype for capturing device telemetry logs and analyzing them to automatically generate bug reproduction steps and conditions.

---

## Directory Structure

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py          # FastAPI application & route definitions
│   ├── models.py        # Pydantic schemas (TelemetryLogInput, LogRecord, BugReport)
│   ├── analyzer.py      # Rule engine analyzing telemetry & generating reports
│   └── storage.py       # In-memory storage repository
├── requirements.txt     # Python dependencies
└── README.md            # Setup and testing instructions
```

---

## Installation & Setup

### 1. Prerequisites
- Python 3.9 or higher

### 2. Create and Activate Virtual Environment

**On Windows (PowerShell):**
```powershell
cd c:\Users\admin\Desktop\bug-reproduction-logger\backend
python -m venv venv
.\venv\Scripts\Activate.ps1
```

**On Linux/macOS:**
```bash
cd backend
python3 -m venv venv
source venv/bin/activate
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

---

## Running the Backend

Run the FastAPI application with Uvicorn bound to `0.0.0.0:8000`:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at:
- **Base URL**: `http://localhost:8000` (or `http://0.0.0.0:8000`)
- **Interactive Swagger Docs**: `http://localhost:8000/docs`
- **ReDoc UI**: `http://localhost:8000/redoc`

---

## API Endpoints & Testing Commands

### 1. Health Check (`GET /`)
Confirms that the backend service is running.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8000/ -Method Get
```

**cURL:**
```bash
curl http://localhost:8000/
```

**Expected Response:**
```json
{
  "status": "running",
  "service": "Bug Reproduction Logger Backend",
  "message": "Bug Reproduction Logger backend is running."
}
```

---

### 2. Submit Telemetry Log (`POST /logs`)
Accepts device telemetry JSON and assigns a unique log ID (`LOG-001`).

**PowerShell:**
```powershell
$body = @{
    battery = 17
    orientation = "landscape"
    network = "weak"
    cpu = 85
    timestamp = (Get-Date).ToString("o")
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8000/logs -Method Post -ContentType "application/json" -Body $body
```

**cURL:**
```bash
curl -X POST http://localhost:8000/logs \
  -H "Content-Type: application/json" \
  -d '{
    "battery": 17,
    "orientation": "landscape",
    "network": "weak",
    "cpu": 85,
    "timestamp": "2026-09-15T18:30:00Z"
  }'
```

**Expected Response:**
```json
{
  "log_id": "LOG-001",
  "message": "Telemetry log received successfully",
  "log": {
    "id": "LOG-001",
    "telemetry": {
      "battery": 17,
      "orientation": "landscape",
      "network": "weak",
      "cpu": 85,
      "timestamp": "2026-09-15T18:30:00Z"
    },
    "created_at": "2026-09-15T18:30:00Z"
  }
}
```

---

### 3. Analyze Log (`POST /analyze/{log_id}`)
Analyzes the stored telemetry using rule-based heuristics and generates a structured bug report.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8000/analyze/LOG-001 -Method Post
```

**cURL:**
```bash
curl -X POST http://localhost:8000/analyze/LOG-001
```

**Expected Response:**
```json
{
  "id": "BUG-001",
  "status": "Bug Reproducible",
  "confidence": 98,
  "summary": "Issue is strongly associated with low battery, landscape orientation, weak network, and high CPU usage.",
  "conditions": {
    "battery": "<17%",
    "orientation": "landscape",
    "network": "weak",
    "cpu": ">85%"
  },
  "steps_to_reproduce": [
    "Open the application",
    "Drain device battery to under 20%",
    "Rotate the device to landscape",
    "Set device network status to weak",
    "Increase CPU load above 80%",
    "Reproduce the reported interaction"
  ],
  "device_context": {
    "battery": 17,
    "orientation": "landscape",
    "network": "weak",
    "cpu": 85,
    "raw_timestamp": "2026-09-15T18:30:00Z"
  },
  "timestamp": "2026-09-15T18:30:05.123456+00:00"
}
```

---

### 4. Get Bug Report (`GET /report/{report_id}`)
Retrieves an existing bug report by ID.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8000/report/BUG-001 -Method Get
```

**cURL:**
```bash
curl http://localhost:8000/report/BUG-001
```

**Expected Response:**
```json
{
  "id": "BUG-001",
  "status": "Bug Reproducible",
  "confidence": 98,
  "summary": "Issue is strongly associated with low battery, landscape orientation, weak network, and high CPU usage.",
  "conditions": {
    "battery": "<17%",
    "orientation": "landscape",
    "network": "weak",
    "cpu": ">85%"
  },
  "steps_to_reproduce": [
    "Open the application",
    "Drain device battery to under 20%",
    "Rotate the device to landscape",
    "Set device network status to weak",
    "Increase CPU load above 80%",
    "Reproduce the reported interaction"
  ],
  "device_context": {
    "battery": 17,
    "orientation": "landscape",
    "network": "weak",
    "cpu": 85,
    "raw_timestamp": "2026-09-15T18:30:00Z"
  },
  "timestamp": "2026-09-15T18:30:05.123456+00:00"
}
```
