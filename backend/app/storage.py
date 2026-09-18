import os
import sqlite3
import threading
from typing import Dict, List, Optional
from app.models import LogRecord, BugReport

class SQLiteStorage:
    def __init__(self, db_path: Optional[str] = None):
        if db_path is None:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            db_path = os.path.join(base_dir, "..", "bugtrace.db")
        self.db_path = os.path.abspath(db_path)
        self._lock = threading.Lock()
        self._init_db()

    def _get_connection(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path, check_same_thread=False)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self) -> None:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS logs (
                        id TEXT PRIMARY KEY,
                        created_at TEXT NOT NULL,
                        data_json TEXT NOT NULL
                    )
                """)
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS reports (
                        id TEXT PRIMARY KEY,
                        log_id TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        data_json TEXT NOT NULL
                    )
                """)
                conn.commit()

    def save_log(self, record: LogRecord) -> None:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                json_str = record.model_dump_json()
                cursor.execute(
                    "INSERT OR REPLACE INTO logs (id, created_at, data_json) VALUES (?, ?, ?)",
                    (record.id, record.created_at, json_str)
                )
                conn.commit()

    def get_log(self, log_id: str) -> Optional[LogRecord]:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT data_json FROM logs WHERE id = ?", (log_id,))
                row = cursor.fetchone()
                if row:
                    try:
                        return LogRecord.model_validate_json(row["data_json"])
                    except Exception:
                        return None
                return None

    def get_all_logs(self) -> List[LogRecord]:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT data_json FROM logs ORDER BY created_at DESC")
                rows = cursor.fetchall()
                results: List[LogRecord] = []
                for r in rows:
                    try:
                        results.append(LogRecord.model_validate_json(r["data_json"]))
                    except Exception:
                        pass
                return results

    def save_report(self, report: BugReport) -> None:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                json_str = report.model_dump_json()
                cursor.execute(
                    "INSERT OR REPLACE INTO reports (id, log_id, timestamp, data_json) VALUES (?, ?, ?, ?)",
                    (report.id, report.log_id or "", report.timestamp, json_str)
                )
                conn.commit()

    def get_report(self, report_id: str) -> Optional[BugReport]:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT data_json FROM reports WHERE id = ? OR log_id = ?", (report_id, report_id))
                row = cursor.fetchone()
                if row:
                    try:
                        return BugReport.model_validate_json(row["data_json"])
                    except Exception:
                        return None
                return None

    def get_all_reports(self) -> List[BugReport]:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT data_json FROM reports ORDER BY timestamp DESC")
                rows = cursor.fetchall()
                results: List[BugReport] = []
                for r in rows:
                    try:
                        results.append(BugReport.model_validate_json(r["data_json"]))
                    except Exception:
                        pass
                return results

    def generate_log_id(self) -> str:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT id FROM logs WHERE id LIKE 'LOG-%'")
                rows = cursor.fetchall()
                max_num = 0
                for row in rows:
                    parts = row["id"].split("-")
                    if len(parts) == 2 and parts[1].isdigit():
                        max_num = max(max_num, int(parts[1]))
                return f"LOG-{max_num + 1:03d}"

    def generate_report_id(self) -> str:
        with self._lock:
            with self._get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT id FROM reports WHERE id LIKE 'BUG-%'")
                rows = cursor.fetchall()
                max_num = 0
                for row in rows:
                    parts = row["id"].split("-")
                    if len(parts) == 2 and parts[1].isdigit():
                        max_num = max(max_num, int(parts[1]))
                return f"BUG-{max_num + 1:03d}"

db = SQLiteStorage()
