FROM python:3.9
WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY parquet_to_json.py .

RUN mkdir -p /data/parquet /data/json  # Create both directories
VOLUME /data/parquet /data/json

CMD ["python", "parquet_to_json.py"]