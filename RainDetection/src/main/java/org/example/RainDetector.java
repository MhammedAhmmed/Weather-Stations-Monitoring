package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.*;
import java.util.concurrent.ExecutionException;


class WeatherData {     // for better parsing
    private Long station_id;
    private Long s_no;
    private String battery_status;
    private Long status_timestamp;
    private Weather weather;

    public Long getStation_id() {
        return station_id;
    }

    public void setStation_id(Long station_id) {
        this.station_id = station_id;
    }

    public Long getS_no() {
        return s_no;
    }

    public void setS_no(Long s_no) {
        this.s_no = s_no;
    }

    public String getBattery_status() {
        return battery_status;
    }

    public void setBattery_status(String battery_status) {
        this.battery_status = battery_status;
    }

    public Long getStatus_timestamp() {
        return status_timestamp;
    }

    public void setStatus_timestamp(Long status_timestamp) {
        this.status_timestamp = status_timestamp;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }
}

class Weather {      // for better manipulation of weather data
    private Integer humidity;
    private Integer temperature;
    private Integer wind_speed;

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public Integer getWind_speed() {
        return wind_speed;
    }

    public void setWind_speed(Integer wind_speed) {
        this.wind_speed = wind_speed;
    }
}


class RainingProcessor extends AbstractProcessor<String, String> {
    private ProcessorContext context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public void process(String key, String value) {
        try {
            WeatherData data = objectMapper.readValue(value, WeatherData.class);
            Integer humidity = data.getWeather().getHumidity();

            if (humidity > 70) {
                String alert = String.format(
                        "{\"station_id\": %d, \"timestamp\": %d, \"humidity\": %d, \"message\": \"Rain detected\"}",
                        data.getStation_id(), data.getStatus_timestamp(), humidity
                );
                context.forward(key, alert); // Use stored context
                context.commit();
            }
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse data: " + e.getMessage());
        }
    }
}

public class RainDetector {
    private static final String INPUT_TOPIC = "weather-status";
    private static final String OUTPUT_TOPIC = "rain-alerts";

    public static void main(String[] args) {
        // Step 1: Configure Kafka Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "rain-detection-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Step 2: Create topics if they don't exist
        createTopicsIfMissing(props);

        // Step 3: Build the Topology (same as before)
        Topology topology = buildTopology();

        // Step 4: Start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static void createTopicsIfMissing(Properties props) {
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> existingTopics = admin.listTopics().names().get();

            List<NewTopic> newTopics = new ArrayList<>();
            if (!existingTopics.contains(INPUT_TOPIC)) {
                newTopics.add(new NewTopic(INPUT_TOPIC, 1, (short) 1));
            }
            if (!existingTopics.contains(OUTPUT_TOPIC)) {
                newTopics.add(new NewTopic(OUTPUT_TOPIC, 1, (short) 1));
            }

            if (!newTopics.isEmpty()) {
                admin.createTopics(newTopics).all().get();
                System.out.println("Created missing topics");
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Topic creation failed: " + e.getMessage());
        }
    }

    private static Topology buildTopology() {
        Topology topology = new Topology();
        topology.addSource("Source", INPUT_TOPIC);
        topology.addProcessor("RainProcessor", RainingProcessor::new, "Source");
        topology.addSink("Sink", OUTPUT_TOPIC, "RainProcessor");
        return topology;
    }
}







