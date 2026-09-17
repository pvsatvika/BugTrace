from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field

class TelemetrySnapshotInput(BaseModel):
    timestamp: Optional[str] = None
    battery: Optional[int] = None
    is_charging: Optional[bool] = None
    orientation: Optional[str] = None
    network: Optional[str] = None
    cpu: Optional[float] = None

class TelemetryLogInput(BaseModel):
    battery: Optional[int] = Field(None, description="Battery level percentage (0-100)")
    orientation: Optional[str] = Field(None, description="Device orientation (e.g. portrait, landscape)")
    network: Optional[str] = Field(None, description="Network connection state (e.g. wifi, cellular, weak, offline)")
    cpu: Optional[float] = Field(None, description="CPU usage percentage (0-100)")
    timestamp: Optional[str] = Field(None, description="Timestamp of telemetry capture")
    is_simulated: Optional[bool] = Field(False, description="Whether this telemetry is simulated demo data")
    telemetry_history: Optional[List[TelemetrySnapshotInput]] = Field(default_factory=list, description="Time-series snapshots")

class LogRecord(BaseModel):
    id: str
    telemetry: TelemetryLogInput
    created_at: str

class BugReport(BaseModel):
    id: str
    report_id: Optional[str] = None
    log_id: Optional[str] = None
    title: Optional[str] = None
    status: str
    confidence: int
    summary: str
    data_source: str = Field("REAL DEVICE TELEMETRY", description="Data source indicator")
    observed_conditions: List[str] = Field(default_factory=list)
    conditions: Dict[str, Any] = Field(default_factory=dict)
    reproduction_steps: List[str] = Field(default_factory=list)
    steps_to_reproduce: List[str] = Field(default_factory=list)
    device_context: Dict[str, Any] = Field(default_factory=dict)
    evidence: List[str] = Field(default_factory=list)
    timestamp: str
    orientation_history: List[str] = Field(default_factory=list)
    orientation_change_count: int = 0
    snapshot_count: int = 0
    score_title: str = Field("CONDITION SCORE", description="Label for condition score")
