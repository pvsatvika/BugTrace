from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException, status
from app.models import TelemetryLogInput, LogRecord, BugReport
from app.storage import db
from app.analyzer import analyze_telemetry

app = FastAPI(
    title="Bug Reproduction Logger Backend",
    description="Hackathon prototype backend for capturing telemetry and analyzing bug reproduction conditions.",
    version="1.0.0"
)

@app.get("/")
def read_root():
    return {
        "status": "running",
        "service": "Bug Reproduction Logger Backend",
        "message": "Bug Reproduction Logger backend is running."
    }

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
