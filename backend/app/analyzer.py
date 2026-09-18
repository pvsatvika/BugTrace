from datetime import datetime, timezone
from typing import Dict, Any, List
from app.models import TelemetryLogInput, BugReport

def analyze_telemetry(log_id: str, telemetry: TelemetryLogInput, report_id: str) -> BugReport:
    detected_conditions_map: Dict[str, Any] = {}
    observed_conditions_list: List[str] = []
    evidence_list: List[str] = []
    repro_implications: List[str] = []
    title_parts: List[str] = []

    # Rule 1: LOW_BATTERY (< 20%)
    if telemetry.battery is not None and telemetry.battery < 20:
        detected_conditions_map["battery"] = f"<{telemetry.battery}%"
        observed_conditions_list.append(f"Battery below 20% ({telemetry.battery}%)")
        evidence_list.append(f"Battery telemetry reported {telemetry.battery}%.")
        repro_implications.append("Maintain device battery level below 20%.")
        title_parts.append("low battery")

    # Rule 2: LANDSCAPE (orientation == "landscape")
    if telemetry.orientation and telemetry.orientation.lower() == "landscape":
        detected_conditions_map["orientation"] = "landscape"
        observed_conditions_list.append("Device in landscape orientation")
        evidence_list.append("Orientation telemetry reported LANDSCAPE.")
        repro_implications.append("Set device to landscape orientation.")
        title_parts.append("landscape orientation")

    # Rule 3: NETWORK (weak or offline)
    if telemetry.network and telemetry.network.lower() in ["weak", "offline"]:
        net_val = telemetry.network.lower()
        detected_conditions_map["network"] = net_val
        observed_conditions_list.append(f"Weak or unavailable network connection ({net_val.upper()})")
        evidence_list.append(f"Network telemetry reported {net_val.upper()}.")
        repro_implications.append(f"Set network connectivity to {net_val.upper()}.")
        title_parts.append(f"{net_val} network")

    # Rule 4: HIGH_CPU (> 80%)
    if telemetry.cpu is not None and telemetry.cpu > 80:
        cpu_val_str = f"{telemetry.cpu:.1f}%" if isinstance(telemetry.cpu, float) else f"{telemetry.cpu}%"
        detected_conditions_map["cpu"] = f">{cpu_val_str}"
        observed_conditions_list.append(f"CPU activity reached {cpu_val_str}, crossing 80% threshold")
        evidence_list.append(f"CPU telemetry reported high load at {cpu_val_str}.")
        repro_implications.append("Maintain CPU activity above 80% threshold.")
        title_parts.append("high CPU load")

    # Capture non-anomalous telemetry evidence for observed device context
    if telemetry.battery is not None and telemetry.battery >= 20:
        evidence_list.append(f"Battery telemetry reported {telemetry.battery}%.")
    if telemetry.orientation and telemetry.orientation.lower() != "landscape":
        evidence_list.append(f"Orientation telemetry reported {telemetry.orientation.upper()}.")
    if telemetry.network and telemetry.network.lower() not in ["weak", "offline"]:
        evidence_list.append(f"Network telemetry observed {telemetry.network}.")
    if telemetry.cpu is not None and telemetry.cpu <= 80:
        cpu_val_str = f"{telemetry.cpu:.1f}%" if isinstance(telemetry.cpu, float) else f"{telemetry.cpu}%"
        evidence_list.append(f"CPU load observed at normal level ({cpu_val_str}).")

    # Inspect telemetry history sequence if available
    history = telemetry.telemetry_history or []
    snapshot_count = len(history)
    orientations_seen: List[str] = []

    if history:
        evidence_list.append(f"{snapshot_count} telemetry snapshots recorded during this capture window.")
        for snap in history:
            if snap.orientation:
                o_str = snap.orientation.upper()
                if not orientations_seen or orientations_seen[-1] != o_str:
                    orientations_seen.append(o_str)
        if len(orientations_seen) > 1:
            transition_str = " -> ".join(orientations_seen)
            evidence_list.append(f"Orientation sequence: {transition_str}.")
    else:
        fallback_o = (telemetry.orientation or "PORTRAIT").upper()
        orientations_seen = [fallback_o]
        snapshot_count = 1

    orientation_change_count = max(0, len(orientations_seen) - 1)

    # Build reproduction guidance sequence
    repro_sequence: List[str] = [
        "Start a capture.",
        "Perform the application action being tested."
    ]
    for step in repro_implications:
        repro_sequence.append(step)
    repro_sequence.append("Observe the resulting application behavior.")

    # Status, Confidence, Title & Summary
    is_simulated = bool(telemetry.is_simulated)
    data_source = "SIMULATED DEMO DATA" if is_simulated else "REAL DEVICE TELEMETRY"
    capture_type_label = "demo capture" if is_simulated else "capture"

    num_conditions = len(detected_conditions_map)
    if num_conditions > 0:
        status = "ANALYZED"
        confidence = min(98, 70 + (num_conditions * 12))
        if num_conditions == 1:
            title = f"{title_parts[0].upper()} DETECTED"
            summary = f"Conditions observed during this {capture_type_label}."
        elif num_conditions == 2:
            title = f"{title_parts[0].upper()} + {title_parts[1].upper()} DETECTED"
            summary = f"Conditions observed during this {capture_type_label}."
        else:
            title = f"MULTIPLE CONDITIONS DETECTED ({num_conditions})"
            summary = f"Conditions observed during this {capture_type_label}."
    else:
        status = "ANALYZED"
        confidence = 50
        title = "NO SIGNIFICANT ANOMALY DETECTED"
        summary = f"No critical telemetry anomaly conditions detected during {capture_type_label} session."
        if not observed_conditions_list:
            observed_conditions_list = ["No threshold conditions breached during capture session."]

    device_context = {
        "battery": f"{telemetry.battery}%" if telemetry.battery is not None else "N/A",
        "charging": False,
        "orientation": (telemetry.orientation or "PORTRAIT").upper(),
        "network": telemetry.network or "Unknown",
        "cpu": f"{telemetry.cpu}%" if telemetry.cpu is not None else "Standard Core Info"
    }

    current_timestamp = datetime.now(timezone.utc).isoformat()

    return BugReport(
        id=report_id,
        report_id=report_id,
        log_id=log_id,
        title=title,
        status=status,
        confidence=confidence,
        summary=summary,
        data_source=data_source,
        observed_conditions=observed_conditions_list,
        conditions=detected_conditions_map,
        reproduction_steps=repro_sequence,
        steps_to_reproduce=repro_sequence,
        device_context=device_context,
        evidence=evidence_list,
        timestamp=current_timestamp,
        orientation_history=orientations_seen,
        orientation_change_count=orientation_change_count,
        snapshot_count=snapshot_count,
        score_title="CONDITION SCORE"
    )
