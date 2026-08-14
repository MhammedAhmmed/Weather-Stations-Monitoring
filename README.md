# 🌦️ Weather Stations Monitoring

A distributed, data-intensive system that simulates a fleet of IoT weather stations streaming
readings through **Apache Kafka** to a **central base station**, which archives every reading
as **Parquet** files and maintains two indexes for querying:

- A **Bitcask**-style key-value store (custom LSM implementation) for the *latest* reading per station
- **ElasticSearch + Kibana** for historical analysis and visualization over the Parquet archive

Built for the *Designing Data Intensive Applications* (CSE-4E3) course project — Alexandria
University, Faculty of Engineering, Computers & Systems Engineering Department.

---

## Architecture

```
Data Acquisition          Data Processing & Archiving        Indexing
─────────────────         ───────────────────────────        ──────────────────
Weather Station 1  ─┐                                     ┌─▶ Bitcask Store
Weather Station 2  ─┤                                     │   (latest reading
Weather Station 3  ─┼──▶  Kafka  ──▶  Central Base Station─┤    per station)
      ...           │                (consume, process,   │
Weather Station 10 ─┘                 batch-write)         └─▶ Parquet Files
                                                                  │
                                                                  ▼
                                                          ElasticSearch / Kibana
                                                          (historical analysis)
```

The system runs as a **Kubernetes** cluster of:

| Component            | Count | Purpose                                   |
|-----------------------|:-----:|--------------------------------------------|
| Weather Station       | 10    | Simulated IoT sensors producing readings   |
| Kafka + Zookeeper     | 1     | Message queue backbone                     |
| Central Base Station  | 1     | Stream consumer, archiver, indexer         |
| ElasticSearch + Kibana| 1     | Historical querying & dashboards           |

---

## ✨ Features

### Weather Station (Producer)
- Emits a status message **every 1 second** via the Kafka Java Producer API
- `battery_status` is randomized per the target distribution: **30% low / 40% medium / 30% high**
- **10%** of messages are randomly dropped to simulate real-world message loss
- Each message carries an auto-incrementing `s_no` per station for ordering/gap detection

**Message schema**

```json
{
  "station_id": 1,
  "s_no": 1,
  "battery_status": "low",
  "status_timestamp": 1681521224,
  "weather": {
    "humidity": 35,
    "temperature": 100,
    "wind_speed": 13
  }
}
```

### Rain Detection (Kafka Streams / DSL)
- A Kafka Processor (Streams DSL) watches incoming readings and flags **humidity > 70%**
- Matching readings are forwarded to a dedicated `rain-alerts` Kafka topic

### Central Base Station (Consumer)
- **Bitcask key-value store** — custom implementation maintaining the latest reading per
  `station_id`:
  - Append-only segment files with in-memory hash index
  - **Hint files** written to speed up recovery/rehydration on restart
  - **Background compaction** scheduled on segment files without blocking active readers
  - No checksums, no tombstones (out of scope per project spec — station IDs are never deleted)
- **Parquet archiving** — every reading is durably archived, **partitioned by time and
  `station_id`**, written in batches (default **10,000 records/batch**) to minimize I/O
  overhead
- **ElasticSearch indexing** — Parquet files are ingested into ElasticSearch for querying via
  Kibana, supporting:
  - Count of low-battery statuses per station
  - Count of dropped messages per station

### Bitcask Client (CLI)
A standalone client for inspecting and load-testing the Bitcask store, callable against the
Central Station's API:

```bash
# Dump all keys/values to a timestamped CSV (e.g. 1746034451.csv)
./bitcask_client.sh --view-all

# Print the value for a single key to stdout
./bitcask_client.sh --view --key=SOME_KEY

# Load test: spin up 100 concurrent readers, each dumping all keys
# to <timestamp>_thread_<n>.csv
./bitcask_client.sh --perf --clients=100
```

### Profiling
The Central Station has been profiled with **Java Flight Recorder (JFR)** over a 1-minute run,
reporting:
- Top 10 classes by total memory
- GC pause count and maximum pause duration
- I/O operations log

See [`docs/jfr-report.md`](docs/jfr-report.md) for the full write-up.

---

## 🛠️ Tech Stack

- **Java** — weather station producers, central station, Kafka Streams processors
- **Apache Kafka** (Bitnami image) + **Zookeeper** — messaging backbone
- **Apache Parquet** — columnar archive format
- **Custom Bitcask (LSM)** — key-value store for latest-state lookups
- **ElasticSearch** + **Kibana** — indexing and visualization
- **Docker** + **Kubernetes** — containerization and orchestration
- **Java Flight Recorder (JFR)** — profiling

---

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/)
- [Kubernetes](https://kubernetes.io/docs/tasks/tools/) (e.g. Minikube, Kind, or Docker Desktop's K8s)
- JDK 17+ and Maven/Gradle (for local builds)


---

## 📊 Validation

The following are validated end-to-end via Kibana dashboards (see `docs/kibana-screenshots/`):

- ✅ Battery status distribution converges to **30% low / 40% medium / 30% high**
- ✅ Message drop rate converges to **~10%**
- ✅ Rain alerts correctly fire when `humidity > 70%`

---

## 📦 Deliverables

- [x] Source code (weather station, central station, Bitcask, Bitcask client)
- [x] Dockerfiles (weather station, central server)
- [x] Kubernetes manifest (`k8s/weather-monitoring.yaml`)
- [x] Kibana screenshots confirming battery distribution and drop rate
- [x] Sample Parquet file (`samples/sample.parquet`)
- [x] Sample Bitcask LSM directory (`samples/bitcask-lsm/`)
- [x] JFR profiling report (`docs/jfr-report.md`)

*Course: CSE-4E3 — Designing Data Intensive Applications*
*Alexandria University — Faculty of Engineering — Computers & Systems Engineering Department*

---

## 📄 License

This project was developed for academic purposes as part of the CSE-4E3 course project.
