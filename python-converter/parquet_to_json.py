import os
import time
from time import sleep

import pandas as pd
import json
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

# Configuration
PARQUET_DIR = os.getenv('PARQUET_DIR', '/data/parquet')
OUTPUT_DIR = os.getenv('OUTPUT_DIR', '/data/json')


class ParquetHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.src_path.endswith(".parquet"):
            print(f"New Parquet file detected: {event.src_path}")
            self.process_parquet(event.src_path)

    def process_parquet(self, parquet_path):
        try:

            file_size = -1
            while True:
                current_size = os.path.getsize(parquet_path)
                if current_size == file_size and current_size > 0:
                    break
                file_size = current_size
                time.sleep(0.5)

            # Read Parquet file
            df = pd.read_parquet(parquet_path)

            # Define output JSON path (same filename, but .json)
            filename = os.path.basename(parquet_path).replace(".parquet", ".json")
            json_path = os.path.join(OUTPUT_DIR, filename)

            # Save as JSON (line-delimited)
            df.to_json(json_path, orient="records", lines=True)

            print(f"Converted: {parquet_path} → {json_path}")
        except Exception as e:
            print(f"Error processing {parquet_path}: {e}")


if __name__ == "__main__":
    # Create output directory if it doesn't exist
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print(f"Monitoring directory: {PARQUET_DIR}")
    print(f"JSON files will be saved to: {OUTPUT_DIR}")

    # Start monitoring
    event_handler = ParquetHandler()
    observer = Observer()
    observer.schedule(event_handler, PARQUET_DIR, recursive=False)
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()