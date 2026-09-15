from datetime import datetime, timezone
from typing import Dict, Any, List
from app.models import TelemetryLogInput, BugReport

def analyze_telemetry(log_id: str, telemetry: TelemetryLogInput, report_id: str) -> BugReport:
    detected_conditions: Dict[str, Any] = {}
    triggered_labels: List[str] = []
    steps: List[str] = ["Open the application"]

    # Rule 1: Battery < 20%
    if telemetry.battery is not None and telemetry.battery < 20:
        detected_conditions["battery"] = f"<{telemetry.battery}%" if telemetry.battery < 20 else "<20%"
        triggered_labels.append("low battery")
        steps.append("Drain device battery to under 20%")

    # Rule 2: Orientation == "landscape"
    if telemetry.orientation and telemetry.orientation.lower() == "landscape":
        detected_conditions["orientation"] = "landscape"
        triggered_labels.append("landscape orientation")
        steps.append("Rotate the device to landscape")

    # Rule 3: Network == "weak" or "offline"
    if telemetry.network and telemetry.network.lower() in ["weak", "offline"]:
        net_val = telemetry.network.lower()
        detected_conditions["network"] = net_val
        triggered_labels.append(f"{net_val} network")
        steps.append(f"Set device network status to {net_val}")

    # Rule 4: CPU > 80%
    if telemetry.cpu is not None and telemetry.cpu > 80:
        detected_conditions["cpu"] = f">{telemetry.cpu}%" if telemetry.cpu > 80 else ">80%"
        triggered_labels.append("high CPU usage")
        steps.append("Increase CPU load above 80%")

    steps.append("Reproduce the reported interaction")

    # Determine status & confidence
    num_conditions = len(detected_conditions)
    if num_conditions > 0:
        status = "Bug Reproducible"
        # Confidence formula: 70 base + 11 per condition up to max 98
        confidence = min(98, 70 + (num_conditions * 11))
        if num_conditions == 1:
            summary = f"Issue is associated with {triggered_labels[0]}."
        elif num_conditions == 2:
            summary = f"Issue is strongly associated with {triggered_labels[0]} and {triggered_labels[1]}."
        else:
            joined = ", ".join(triggered_labels[:-1]) + f", and {triggered_labels[-1]}"
            summary = f"Issue is strongly associated with {joined}."
    else:
        status = "No Anomaly Detected"
        confidence = 50
        summary = "No critical telemetry anomaly conditions detected."

    device_context = {
        "battery": telemetry.battery,
        "orientation": telemetry.orientation,
        "network": telemetry.network,
        "cpu": telemetry.cpu,
        "raw_timestamp": telemetry.timestamp
    }

    current_timestamp = datetime.now(timezone.utc).isoformat()

    return BugReport(
        id=report_id,
        status=status,
        confidence=confidence,
        summary=summary,
        conditions=detected_conditions,
        steps_to_reproduce=steps,
        device_context=device_context,
        timestamp=current_timestamp
    )
