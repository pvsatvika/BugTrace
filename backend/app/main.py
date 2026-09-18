import os
from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException, status
from fastapi.responses import HTMLResponse, FileResponse
from app.models import TelemetryLogInput, TelemetrySnapshotInput, LogRecord, BugReport
from app.storage import db
from app.analyzer import analyze_telemetry

app = FastAPI(
    title="Bug Reproduction Logger Backend",
    description="Hackathon prototype backend for capturing telemetry and analyzing bug reproduction conditions.",
    version="1.0.0"
)

STATIC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static")
DASHBOARD_FILE = os.path.join(STATIC_DIR, "dashboard.html")

@app.get("/")
def read_root():
    return {
        "status": "running",
        "service": "Bug Reproduction Logger Backend",
        "message": "Bug Reproduction Logger backend is running.",
        "dashboard": "/dashboard"
    }

@app.get("/dashboard", response_class=HTMLResponse)
def get_dashboard():
    if os.path.exists(DASHBOARD_FILE):
        return FileResponse(DASHBOARD_FILE)
    return HTMLResponse(content="<h1>Dashboard file not found</h1>", status_code=404)

@app.post("/logs", status_code=status.HTTP_201_CREATED)
def create_log(telemetry: TelemetryLogInput):
    log_id = db.generate_log_id()
    timestamp = telemetry.timestamp or datetime.now(timezone.utc).isoformat()
    record = LogRecord(
        id=log_id,
        telemetry=telemetry,
        created_at=timestamp
    )
    db.save_log(record)
    return {
        "log_id": log_id,
        "message": "Telemetry log received successfully",
        "log": record
    }

@app.post("/analyze/{log_id}")
def analyze_log(log_id: str):
    record = db.get_log(log_id)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Telemetry log with ID '{log_id}' not found."
        )

    report_id = db.generate_report_id()
    report = analyze_telemetry(log_id=log_id, telemetry=record.telemetry, report_id=report_id)
    db.save_report(report)
    return report

@app.get("/report/{report_id}")
def get_report(report_id: str):
    report = db.get_report(report_id)
    if not report:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Bug report with ID '{report_id}' not found."
        )
    return report

# DASHBOARD API ENDPOINTS
@app.get("/api/stats")
def get_stats():
    logs = db.get_all_logs()
    reports = db.get_all_reports()
    latest_timestamp = logs[0].created_at if logs else None
    return {
        "captures_count": len(logs),
        "reports_count": len(reports),
        "latest_timestamp": latest_timestamp,
        "status": "running"
    }

@app.get("/api/captures")
def list_captures():
    return db.get_all_logs()

@app.get("/api/captures/{log_id}")
def get_capture(log_id: str):
    record = db.get_log(log_id)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Telemetry log with ID '{log_id}' not found."
        )
    return record

@app.get("/api/reports")
def list_reports():
    return db.get_all_reports()

@app.post("/api/demo/trigger/{scenario}", status_code=status.HTTP_201_CREATED)
def trigger_demo_scenario(scenario: str):
    now_iso = datetime.now(timezone.utc).isoformat()
    sc_clean = scenario.lower()

    if sc_clean == "low_battery":
        telemetry = TelemetryLogInput(
            battery=15,
            orientation="portrait",
            network="wifi",
            cpu=45.0,
            timestamp=now_iso,
            is_simulated=True,
            telemetry_history=[
                TelemetrySnapshotInput(timestamp=now_iso, battery=25, is_charging=False, orientation="portrait", network="wifi", cpu=40.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=15, is_charging=False, orientation="portrait", network="wifi", cpu=45.0)
            ]
        )
    elif sc_clean == "landscape":
        telemetry = TelemetryLogInput(
            battery=80,
            orientation="landscape",
            network="wifi",
            cpu=40.0,
            timestamp=now_iso,
            is_simulated=True,
            telemetry_history=[
                TelemetrySnapshotInput(timestamp=now_iso, battery=80, is_charging=False, orientation="portrait", network="wifi", cpu=35.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=80, is_charging=False, orientation="landscape", network="wifi", cpu=40.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=79, is_charging=False, orientation="portrait", network="wifi", cpu=38.0)
            ]
        )
    elif sc_clean == "weak_network":
        telemetry = TelemetryLogInput(
            battery=75,
            orientation="portrait",
            network="weak",
            cpu=40.0,
            timestamp=now_iso,
            is_simulated=True,
            telemetry_history=[
                TelemetrySnapshotInput(timestamp=now_iso, battery=75, is_charging=False, orientation="portrait", network="wifi", cpu=35.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=75, is_charging=False, orientation="portrait", network="weak", cpu=40.0)
            ]
        )
    elif sc_clean == "high_cpu":
        telemetry = TelemetryLogInput(
            battery=65,
            orientation="portrait",
            network="wifi",
            cpu=88.0,
            timestamp=now_iso,
            is_simulated=True,
            telemetry_history=[
                TelemetrySnapshotInput(timestamp=now_iso, battery=65, is_charging=False, orientation="portrait", network="wifi", cpu=45.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=64, is_charging=False, orientation="portrait", network="wifi", cpu=88.0)
            ]
        )
    elif sc_clean in ["multi", "low_battery_landscape"]:
        telemetry = TelemetryLogInput(
            battery=12,
            orientation="landscape",
            network="weak",
            cpu=85.0,
            timestamp=now_iso,
            is_simulated=True,
            telemetry_history=[
                TelemetrySnapshotInput(timestamp=now_iso, battery=18, is_charging=False, orientation="portrait", network="wifi", cpu=50.0),
                TelemetrySnapshotInput(timestamp=now_iso, battery=12, is_charging=False, orientation="landscape", network="weak", cpu=85.0)
            ]
        )
    else:
        raise HTTPException(status_code=400, detail=f"Unknown scenario '{scenario}'")

    log_id = db.generate_log_id()
    record = LogRecord(id=log_id, telemetry=telemetry, created_at=now_iso)
    db.save_log(record)

    report_id = db.generate_report_id()
    report = analyze_telemetry(log_id=log_id, telemetry=telemetry, report_id=report_id)
    db.save_report(report)

    return {
        "message": f"Demo scenario '{scenario}' executed successfully",
        "log": record,
        "report": report
    }
