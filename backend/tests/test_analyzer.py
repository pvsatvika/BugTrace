import unittest
from app.models import TelemetryLogInput, TelemetrySnapshotInput, TelemetryEventInput
from app.analyzer import analyze_telemetry

class TestAnalyzer(unittest.TestCase):
    def test_normal_real_device_telemetry(self):
        telemetry = TelemetryLogInput(
            battery=85,
            orientation="portrait",
            network="wifi",
            cpu=14.5,
            is_simulated=False,
            telemetry_history=[
                TelemetrySnapshotInput(battery=85, orientation="portrait", network="wifi", cpu=14.5)
            ]
        )
        report = analyze_telemetry("log-1", telemetry, "REP-001")
        self.assertEqual(report.title, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.detection_status, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.confidence, 100)
        self.assertEqual(report.data_source, "REAL DEVICE TELEMETRY")
        self.assertEqual(len(report.conditions), 0)
        self.assertTrue(any("normal level" in e for e in report.evidence))

    def test_app_crash_v1_demo_scenario(self):
        # Scenario: ShopDemo V1 landscape crash
        telemetry = TelemetryLogInput(
            battery=80,
            orientation="landscape",
            network="wifi",
            cpu=25.0,
            is_simulated=False,
            events=[
                TelemetryEventInput(event_type="CAPTURE_START", description="User started capture"),
                TelemetryEventInput(event_type="ORIENTATION_CHANGE", description="Orientation changed to LANDSCAPE"),
                TelemetryEventInput(
                    event_type="APP_CRASH",
                    description="Application crash detected for com.example.shopdemo.v1",
                    details={
                        "target_package": "com.example.shopdemo.v1",
                        "exit_reason": "UNHANDLED_EXCEPTION",
                        "last_orientation": "LANDSCAPE"
                    }
                )
            ],
            telemetry_history=[
                TelemetrySnapshotInput(battery=80, orientation="portrait", network="wifi", cpu=15.0),
                TelemetrySnapshotInput(battery=80, orientation="landscape", network="wifi", cpu=25.0)
            ]
        )
        report = analyze_telemetry("log-crash", telemetry, "REP-CRASH")
        self.assertEqual(report.title, "APPLICATION CRASH DETECTED")
        self.assertEqual(report.detection_status, "ANOMALY DETECTED")
        self.assertEqual(report.confidence, 98)
        self.assertIn("app_crash", report.conditions)
        self.assertTrue(any("com.example.shopdemo.v1" in step for step in report.reproduction_steps))
        self.assertTrue(any("landscape" in step.lower() for step in report.reproduction_steps))

    def test_shopdemo_v2_landscape_survival_scenario(self):
        # Scenario: ShopDemo V2 rotated to landscape with NO crash
        telemetry = TelemetryLogInput(
            battery=82,
            orientation="landscape",
            network="wifi",
            cpu=18.0,
            is_simulated=False,
            events=[
                TelemetryEventInput(event_type="CAPTURE_START", description="User started capture"),
                TelemetryEventInput(event_type="ORIENTATION_CHANGE", description="Orientation changed to LANDSCAPE")
            ],
            telemetry_history=[
                TelemetrySnapshotInput(battery=82, orientation="portrait", network="wifi", cpu=15.0),
                TelemetrySnapshotInput(battery=82, orientation="landscape", network="wifi", cpu=18.0)
            ]
        )
        report = analyze_telemetry("log-v2", telemetry, "REP-V2")
        self.assertEqual(report.title, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.detection_status, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.confidence, 100)
        self.assertNotIn("app_crash", report.conditions)

    def test_high_cpu_condition(self):
        telemetry = TelemetryLogInput(
            battery=70,
            orientation="portrait",
            network="wifi",
            cpu=88.5,
            is_simulated=False
        )
        report = analyze_telemetry("log-2", telemetry, "REP-002")
        self.assertEqual(report.title, "HIGH CPU LOAD DETECTED")
        self.assertEqual(report.detection_status, "ANOMALY DETECTED")
        self.assertIn("cpu", report.conditions)
        self.assertGreaterEqual(report.confidence, 80)

    def test_low_battery_condition(self):
        telemetry = TelemetryLogInput(
            battery=12,
            orientation="portrait",
            network="wifi",
            cpu=15.0,
            is_simulated=False
        )
        report = analyze_telemetry("log-3", telemetry, "REP-003")
        self.assertEqual(report.title, "LOW BATTERY DETECTED")
        self.assertEqual(report.detection_status, "ANOMALY DETECTED")
        self.assertIn("battery", report.conditions)

    def test_weak_network_condition(self):
        telemetry = TelemetryLogInput(
            battery=60,
            orientation="portrait",
            network="weak",
            cpu=20.0,
            is_simulated=False
        )
        report = analyze_telemetry("log-4", telemetry, "REP-004")
        self.assertEqual(report.title, "WEAK NETWORK DETECTED")
        self.assertEqual(report.detection_status, "ANOMALY DETECTED")
        self.assertIn("network", report.conditions)

    def test_simulated_demo_data_badge(self):
        telemetry = TelemetryLogInput(
            battery=15,
            orientation="portrait",
            network="wifi",
            cpu=45.0,
            is_simulated=True
        )
        report = analyze_telemetry("log-7", telemetry, "REP-007")
        self.assertEqual(report.data_source, "SIMULATED DEMO DATA")

    def test_malformed_and_missing_telemetry_fields(self):
        telemetry = TelemetryLogInput(
            battery=None,
            orientation=None,
            network=None,
            cpu=None,
            is_simulated=False
        )
        report = analyze_telemetry("log-8", telemetry, "REP-008")
        self.assertEqual(report.title, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.detection_status, "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report.status, "ANALYZED")

    def test_events_and_orientation_sequence_timeline(self):
        telemetry = TelemetryLogInput(
            battery=80,
            orientation="landscape",
            network="wifi",
            cpu=22.0,
            events=[
                TelemetryEventInput(event_type="CAPTURE_START", description="User tapped start capture")
            ],
            telemetry_history=[
                TelemetrySnapshotInput(orientation="portrait"),
                TelemetrySnapshotInput(orientation="landscape")
            ]
        )
        report = analyze_telemetry("log-9", telemetry, "REP-009")
        self.assertGreaterEqual(len(report.event_timeline), 2)
        self.assertEqual(report.orientation_change_count, 1)

if __name__ == '__main__':
    unittest.main()
