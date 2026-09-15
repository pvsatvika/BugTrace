from typing import Dict, Optional
from app.models import LogRecord, BugReport

class MemoryStorage:
    def __init__(self):
        self._logs: Dict[str, LogRecord] = {}
        self._reports: Dict[str, BugReport] = {}
        self._log_counter = 0
        self._report_counter = 0

    def save_log(self, record: LogRecord) -> None:
        self._logs[record.id] = record

    def get_log(self, log_id: str) -> Optional[LogRecord]:
        return self._logs.get(log_id)

    def save_report(self, report: BugReport) -> None:
        self._reports[report.id] = report

    def get_report(self, report_id: str) -> Optional[BugReport]:
        return self._reports.get(report_id)

    def generate_log_id(self) -> str:
        self._log_counter += 1
        return f"LOG-{self._log_counter:03d}"

    def generate_report_id(self) -> str:
        self._report_counter += 1
        return f"BUG-{self._report_counter:03d}"

db = MemoryStorage()
