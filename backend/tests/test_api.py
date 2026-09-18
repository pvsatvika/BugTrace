import unittest
from fastapi.testclient import TestClient
from app.main import app

class TestApiEndpoints(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_root_endpoint(self):
        response = self.client.get("/")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "running")
        self.assertIn("dashboard", data)

    def test_dashboard_endpoint(self):
        response = self.client.get("/dashboard")
        self.assertEqual(response.status_code, 200)
        self.assertIn("text/html", response.headers["content-type"])

    def test_create_and_analyze_log(self):
        payload = {
            "battery": 82,
            "orientation": "portrait",
            "network": "wifi",
            "cpu": 15.2,
            "is_simulated": False
        }
        log_res = self.client.post("/logs", json=payload)
        self.assertEqual(log_res.status_code, 201)
        log_data = log_res.json()
        self.assertIn("log_id", log_data)
        log_id = log_data["log_id"]

        analyze_res = self.client.post(f"/analyze/{log_id}")
        self.assertEqual(analyze_res.status_code, 200)
        report = analyze_res.json()
        self.assertEqual(report["title"], "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report["detection_status"], "NO SIGNIFICANT ANOMALY DETECTED")
        self.assertEqual(report["data_source"], "REAL DEVICE TELEMETRY")

    def test_demo_scenario_trigger(self):
        res = self.client.post("/api/demo/trigger/high_cpu")
        self.assertEqual(res.status_code, 201)
        data = res.json()
        self.assertEqual(data["report"]["title"], "HIGH CPU LOAD DETECTED")
        self.assertEqual(data["report"]["detection_status"], "ANOMALY DETECTED")
        self.assertEqual(data["report"]["data_source"], "SIMULATED DEMO DATA")

    def test_get_nonexistent_log(self):
        res = self.client.get("/api/captures/nonexistent-id")
        self.assertEqual(res.status_code, 404)

    def test_get_nonexistent_report(self):
        res = self.client.get("/report/nonexistent-id")
        self.assertEqual(res.status_code, 404)

if __name__ == '__main__':
    unittest.main()
