package org.example;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JsonToParquetToElasticsearch {

    private static final int NUM_STATIONS = 10;
    private static final int BATCH_SIZE = 10000;
    private static final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private static final String TOPIC = "weather-status";
    private static final String BOOTSTRAP_SERVERS = "kafka.weather.svc.cluster.local:9092";
    private static final String GROUP_ID = "parquet-writer-group";

    // Map to store messages by station ID
    private static final Map<Long, List<GenericRecord>> stationMessages = new ConcurrentHashMap<>();
    private static final Object flushLock = new Object();

    public static void main(String[] args) {
        // Initialize empty lists for each station
        for (long i = 1; i <= NUM_STATIONS; i++) {
            stationMessages.put(i, Collections.synchronizedList(new ArrayList<>()));
        }

        // Create data directory
        try {
            Files.createDirectories(Paths.get("data/parquet"));
        } catch (IOException e) {
            System.err.println("Failed to create data directory:");
            e.printStackTrace();
            return;
        }

        // Configure Kafka consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));

        // Safety flush every 5 minutes
        ScheduledExecutorService flusher = Executors.newScheduledThreadPool(1);
        flusher.scheduleAtFixedRate(JsonToParquetToElasticsearch::flushAllStations, 5, 5, TimeUnit.MINUTES);

        // Main consumption loop
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    GenericRecord message = parseMessage(record.value());
                    if (message != null) {
                        long stationId = (long) message.get("station_id");
                        stationMessages.get(stationId).add(message);

                        if (totalMessagesReceived.incrementAndGet() % BATCH_SIZE == 0) {
                            flushAllStations();
                        }
                    }
                }
                consumer.commitSync();
            }
        } finally {
            consumer.close();
            flusher.shutdown();
        }
    }

    private static GenericRecord parseMessage(String json) {
        try {
            String schemaJson = "{\"type\":\"record\",\"name\":\"StationReading\",\"fields\":[" +
                    "{\"name\":\"station_id\",\"type\":\"long\"}," +
                    "{\"name\":\"s_no\",\"type\":\"long\"}," +
                    "{\"name\":\"battery_status\",\"type\":\"string\"}," +
                    "{\"name\":\"status_timestamp\",\"type\":\"long\"}," +
                    "{\"name\":\"weather\",\"type\":{\"type\":\"record\",\"name\":\"WeatherData\",\"fields\":[" +
                    "{\"name\":\"humidity\",\"type\":\"int\"}," +
                    "{\"name\":\"temperature\",\"type\":\"int\"}," +
                    "{\"name\":\"wind_speed\",\"type\":\"int\"}]}}]}";

            Schema schema = new Schema.Parser().parse(schemaJson);
            // Implement JSON to GenericRecord conversion here
            // For simplicity, we'll use a placeholder
            GenericRecord record = new GenericData.Record(schema);
            // Parse your JSON and populate the record
            return record;
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
            return null;
        }
    }
    private static void flushAllStations() {
        synchronized (flushLock) {
            long batchTimestamp = Instant.now().getEpochSecond();

            stationMessages.forEach((stationId, messages) -> {
                if (!messages.isEmpty()) {
                    try {
                        writeStationBatch(stationId, new ArrayList<>(messages), batchTimestamp);
                        messages.clear();
                    } catch (IOException e) {
                        System.err.println("Error writing station " + stationId + " data:");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("Flushed all stations at " + batchTimestamp);
        }
    }
    private static void writeStationBatch(long stationId, List<GenericRecord> messages, long batchTimestamp)
            throws IOException {
        String fileName = String.format("station_%d_%d.parquet", stationId, batchTimestamp);
        File parquetFile = new File("data/parquet", fileName);

        String schemaJson = "{\"type\":\"record\",\"name\":\"StationReading\",\"fields\":[" +
                "{\"name\":\"station_id\",\"type\":\"long\"}," +
                "{\"name\":\"s_no\",\"type\":\"long\"}," +
                "{\"name\":\"battery_status\",\"type\":\"string\"}," +
                "{\"name\":\"status_timestamp\",\"type\":\"long\"}," +
                "{\"name\":\"weather\",\"type\":{\"type\":\"record\",\"name\":\"WeatherData\",\"fields\":[" +
                "{\"name\":\"humidity\",\"type\":\"int\"}," +
                "{\"name\":\"temperature\",\"type\":\"int\"}," +
                "{\"name\":\"wind_speed\",\"type\":\"int\"}]}}]}";

        Schema schema = new Schema.Parser().parse(schemaJson);

        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(
                        new org.apache.hadoop.fs.Path(parquetFile.getAbsolutePath()))
                .withSchema(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {

            for (GenericRecord message : messages) {
                writer.write(message);
            }
            System.out.printf("Written %,d messages for station %d to %s%n",
                    messages.size(), stationId, fileName);
        }
    }

    // Rest of the methods (flushAllStations, writeStationBatch) remain the same as before
    // Only change the class name in the logs if needed
}