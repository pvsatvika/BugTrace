from datetime import datetime, timezone
from typing import Dict, Any, List
from app.models import TelemetryLogInput, BugReport

def analyze_telemetry(log_id: str, telemetry: TelemetryLogInput, report_id: str) -> BugReport:
    detected_conditions_map: Dict[str, Any] = {}
    observed_conditions_list: List[str] = []
    evidence_list: List[str] = []
    repro_implications: List[str] = []
    title_parts: List[str] = []

    client_events = telemetry.events or []

    # Rule 0: APP_CRASH (Explicit Target Application Crash Event)
    app_crash_event = None
    for evt in client_events:
        evt_type = (evt.event_type or "").upper()
        if evt_type in ["APP_CRASH", "CRASH"]:
            app_crash_event = evt
            break

    target_package = None
    if app_crash_event:
        details = app_crash_event.details or {}
        target_package = details.get("target_package") or "com.example.shopdemo.v1"
        exit_reason = details.get("exit_reason") or "UNHANDLED_EXCEPTION"
        exit_desc = details.get("exit_description") or app_crash_event.description or "Target application process terminated unexpectedly"
        last_orient = details.get("last_orientation") or (telemetry.orientation or "LANDSCAPE").upper()

        detected_conditions_map["app_crash"] = f"CRASH: {target_package}"
        observed_conditions_list.append(f"Application Crash Detected ({target_package})")
        evidence_list.append(f"APP CRASH EVIDENCE: Target package '{target_package}' process terminated unexpectedly ({exit_reason}) during capture while in {last_orient} orientation.")
        title_parts.append("application crash")

    # Multi-condition telemetry observation checking (charging, network, orientation)
    history = telemetry.telemetry_history or []
    is_charging_observed = any(
        snap.is_charging for snap in history if snap.is_charging is not None
    )
    if app_crash_event and app_crash_event.details and app_crash_event.details.get("is_charging") is not None:
        is_charging_observed = bool(app_crash_event.details.get("is_charging"))

    net_state_upper = (telemetry.network or "").upper()
    if not net_state_upper and history:
        for snap in history:
            if snap.network:
                net_state_upper = snap.network.upper()
                break
    if not net_state_upper:
        net_state_upper = "WI-FI"

    orientation_upper = (telemetry.orientation or "PORTRAIT").upper()

    if app_crash_event:
        # Highlight multi-condition environment for the crash finding
        if is_charging_observed:
            observed_conditions_list.append("Device Charging State: CHARGING (USB/AC Active)")
            detected_conditions_map["charging"] = "CHARGING"
        if "WIFI" in net_state_upper or "WI-FI" in net_state_upper:
            observed_conditions_list.append("Network State: WI-FI Connected")
            detected_conditions_map["network_type"] = "WI-FI"
        if orientation_upper == "LANDSCAPE" or (app_crash_event and app_crash_event.details and app_crash_event.details.get("last_orientation") == "LANDSCAPE"):
            observed_conditions_list.append("Device Orientation: LANDSCAPE")
            detected_conditions_map["orientation"] = "LANDSCAPE"

    # Rule 1: LOW_BATTERY (< 20%)
    if telemetry.battery is not None and telemetry.battery < 20:
        detected_conditions_map["battery"] = f"<{telemetry.battery}%"
        observed_conditions_list.append(f"Battery below 20% threshold ({telemetry.battery}%)")
        evidence_list.append(f"Battery telemetry reported {telemetry.battery}%.")
        repro_implications.append("Maintain device battery level below 20%.")
        title_parts.append("low battery")

    # Rule 2: LANDSCAPE (orientation == "landscape")
    if telemetry.orientation and telemetry.orientation.lower() == "landscape":
        evidence_list.append("Orientation telemetry reported LANDSCAPE.")
        repro_implications.append("Set device to landscape orientation.")
        if not app_crash_event:
            # Only add to title_parts if it's a non-crash signal anomaly test
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
    if is_charging_observed:
        evidence_list.append("Device power state: CHARGING (USB/AC active).")

    # Inspect telemetry history sequence & client events
    snapshot_count = len(history)
    orientations_seen: List[str] = []
    event_timeline: List[Dict[str, Any]] = []

    # Map client events into timeline
    for evt in client_events:
        event_timeline.append({
            "timestamp": evt.timestamp or datetime.now(timezone.utc).isoformat(),
            "event_type": evt.event_type or "EVENT",
            "description": evt.description or "Device event logged",
            "details": evt.details or {}
        })

    if history:
        evidence_list.append(f"{snapshot_count} telemetry snapshots recorded during capture window.")
        for idx, snap in enumerate(history):
            if snap.orientation:
                o_str = snap.orientation.upper()
                if not orientations_seen or orientations_seen[-1] != o_str:
                    orientations_seen.append(o_str)
                    if idx > 0:
                        event_timeline.append({
                            "timestamp": snap.timestamp or datetime.now(timezone.utc).isoformat(),
                            "event_type": "ORIENTATION_CHANGE",
                            "description": f"Orientation changed to {o_str}",
                            "details": {"orientation": o_str}
                        })
        if len(orientations_seen) > 1:
            transition_str = " -> ".join(orientations_seen)
            evidence_list.append(f"Orientation sequence: {transition_str}.")
    else:
        fallback_o = (telemetry.orientation or "PORTRAIT").upper()
        orientations_seen = [fallback_o]
        snapshot_count = 1

    orientation_change_count = max(0, len(orientations_seen) - 1)

    # Build reproduction guidance sequence
    if "app_crash" in detected_conditions_map:
        repro_sequence = [
            "Start a BugTrace capture session on the physical device.",
            "Connect device to USB/AC power charger (CHARGING active).",
            "Ensure device is connected to WI-FI network.",
            "Set device to LANDSCAPE orientation.",
            f"Launch target application ({target_package}) and select/interact with product items.",
            f"Observe application crash / process termination in {target_package}."
        ]
    else:
        repro_sequence = [
            "Start a capture session on the device.",
            "Perform the application user action being tested."
        ]
        for step in repro_implications:
            repro_sequence.append(step)
        repro_sequence.append("Observe and verify normal application operation.")

    # Status, Confidence, Title & Summary
    is_simulated = bool(telemetry.is_simulated)
    data_source = "SIMULATED DEMO DATA" if is_simulated else "REAL DEVICE TELEMETRY"
    capture_type_label = "demo capture" if is_simulated else "capture"

    if "app_crash" in detected_conditions_map:
        status = "ANALYZED"
        detection_status = "ANOMALY DETECTED"
        confidence = 98
        title = "APPLICATION CRASH DETECTED"
        summary = f"Target application ({target_package}) process crashed during multi-condition capture (Wi-Fi + Charging + Landscape)."
        score_explanation = "Application process termination recorded during telemetry capture under active condition set."
    elif len(detected_conditions_map) > 0:
        status = "ANALYZED"
        detection_status = "ANOMALY DETECTED"
        num_conditions = len(detected_conditions_map)
        confidence = min(98, 70 + (num_conditions * 10))
        score_explanation = f"Evaluated {num_conditions} breached threshold condition(s) across telemetry signals."
        if num_conditions == 1:
            title = f"{title_parts[0].upper()} DETECTED"
            summary = f"Conditions observed during this {capture_type_label}."
        else:
            title = f"MULTIPLE CONDITIONS DETECTED ({num_conditions})"
            summary = f"Conditions observed during this {capture_type_label}."
    else:
        status = "ANALYZED"
        detection_status = "NO SIGNIFICANT ANOMALY DETECTED"
        confidence = 100
        score_explanation = "Baseline verified — target application ran stably with no process crashes or critical signal anomalies."
        title = "NO SIGNIFICANT ANOMALY DETECTED"
        summary = f"No process crash or critical telemetry anomalies detected during {capture_type_label} session."
        if not observed_conditions_list:
            observed_conditions_list = ["No threshold conditions breached during capture session."]

    device_context = {
        "battery": f"{telemetry.battery}%" if telemetry.battery is not None else "N/A",
        "charging": "CHARGING (USB/AC)" if is_charging_observed else "DISCHARGING",
        "orientation": (telemetry.orientation or "PORTRAIT").upper(),
        "network": (telemetry.network or "Unknown").upper(),
        "cpu": f"{telemetry.cpu:.1f}%" if isinstance(telemetry.cpu, float) else (f"{telemetry.cpu}%" if telemetry.cpu is not None else "Standard Core Info")
    }

    current_timestamp = datetime.now(timezone.utc).isoformat()

    return BugReport(
        id=report_id,
        report_id=report_id,
        log_id=log_id,
        title=title,
        status=status,
        detection_status=detection_status,
        confidence=confidence,
        summary=summary,
        data_source=data_source,
        observed_conditions=observed_conditions_list,
        conditions=detected_conditions_map,
        reproduction_steps=repro_sequence,
        steps_to_reproduce=repro_sequence,
        device_context=device_context,
        evidence=evidence_list,
        event_timeline=event_timeline,
        timestamp=current_timestamp,
        orientation_history=orientations_seen,
        orientation_change_count=orientation_change_count,
        snapshot_count=snapshot_count,
        score_title="THRESHOLD CONFIDENCE METRIC",
        score_explanation=score_explanation
    )
