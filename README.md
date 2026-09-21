# BugTrace

### Android Device Telemetry · Automated Bug Detection · Developer Reproduction Reports

BugTrace is an end-to-end diagnostic system built for the **iQOO Hackathon**. It captures telemetry and user-action evidence from a physical Android device, sends the captured data to a local FastAPI backend, analyzes multiple signals, and generates structured, developer-readable bug reproduction reports.

The system is designed to turn **device behavior into actionable developer evidence**.

---

## Key Features

- **Real Device Telemetry** — Captures battery, charging state, network state, orientation, CPU activity, and telemetry snapshots.
- **Multi-Signal Analysis** — Detects conditions such as high CPU, low battery, weak network, and landscape orientation.
- **Demo Scenarios** — Provides controlled scenarios for demonstrating individual and combined conditions.
- **Developer Console** — Displays captures, reports, timelines, telemetry evidence, and reproduction conditions.
- **Android → Backend Pipeline** — Sends telemetry from the Android device to the Windows backend over local Wi-Fi using HTTP/JSON.
- **Persistent Evidence** — Stores telemetry and generated reports in SQLite.
- **Reproduction Guidance** — Converts detected conditions into structured reproduction steps.

---

# System Architecture

```text
┌─────────────────────────────────┐
│       Physical Android Device   │
│                                 │
│  • Battery & charging           │
│  • Network state                │
│  • Orientation                  │
│  • CPU activity                 │
│  • Telemetry snapshots          │
│  • User actions                 │
└───────────────┬─────────────────┘
                │
                │ Wi-Fi
                │ HTTP / JSON
                ▼
┌─────────────────────────────────┐
│      BugTrace Windows Backend   │
│                                 │
│  FastAPI                        │
│  SQLite                         │
│  Telemetry Analyzer             │
│  Report Generator               │
└───────────────┬─────────────────┘
                │
                │ HTTP
                ▼
┌─────────────────────────────────┐
│      BugTrace Developer Console │
│                                 │
│  • Dashboard                    │
│  • Captures                     │
│  • Reports                      │
│  • Timelines                    │
│  • Demo Scenarios               │
└─────────────────────────────────┘
