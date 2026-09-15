from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
from datetime import datetime, timezone

class TelemetryLogInput(BaseModel):
    battery: Optional[int] = Field(None, description="Battery level percentage (0-100)")
    orientation: Optional[str] = Field(None, description="Device orientation (e.g. portrait, landscape)")
    network: Optional[str] = Field(None, description="Network connection state (e.g. wifi, cellular, weak, offline)")
    cpu: Optional[float] = Field(None, description="CPU usage percentage (0-100)")
    timestamp: Optional[str] = Field(None, description="Timestamp of telemetry capture")

class LogRecord(BaseModel):
    id: str
    telemetry: TelemetryLogInput
    created_at: str

class BugReport(BaseModel):
    id: str
    status: str
    confidence: int
    summary: str
    conditions: Dict[str, Any]
    steps_to_reproduce: List[str]
    device_context: Dict[str, Any]
    timestamp: str
