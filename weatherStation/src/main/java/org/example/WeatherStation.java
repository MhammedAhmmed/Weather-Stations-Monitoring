package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

public class WeatherStation {

    private long station_id;
    private long s_no=0;   // incremental with messages
    private String battery_status;   //randomly set by a function
    private long status_timestamp;
    private int humidity;     //percentage
    private int temperature;   //fahrenheit
    private int wind_speed;    // km/h

    private static final double DROP_RATE = 0.10;
    private static final ObjectMapper MAPPER = new ObjectMapper();



    public void setStation_id(long station_id) {
        this.station_id = station_id;
    }

    public void setS_no() {
        this.s_no++;
    }

    public void setBattery_status() {
        double rand=Math.random();
        if(rand<0.3) {
            this.battery_status = "low";
        }
        else if (rand< 0.7) {
            this.battery_status = "medium";
        }
        else
        {
            this.battery_status = "high";
        }


    }


    public void setStatus_timestamp() {
        this.status_timestamp = System.currentTimeMillis() / 1000L;
    }

    public void setHumidity() {
        this.humidity = (int) (Math.random()*100);
    }

    public void setTemperature() {
        this.temperature = (int) (Math.random()*115);
    }

    public void setWind_speed() {
        this.wind_speed = (int) (Math.random()*100);
    }

    private KafkaProducer<String, String> producer;

    public void initProducer() {
        // Read bootstrap servers from env-var, default to localhost:9092
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            bootstrapServers = "localhost:9092";
        }
        String topicName = "weather-status";

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(props);

        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            if (!existingTopics.contains(topicName)) {
                System.out.println("Creating topic '" + topicName + "'...");
                NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                System.out.println("Topic '" + topicName + "' created.");
            } else {
                System.out.println("Topic '" + topicName + "' already exists.");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }



    public void sendMessage()
    {
        setS_no();
        setBattery_status();
        setStatus_timestamp();
        setHumidity();
        setTemperature();
        setWind_speed();

        double rand=Math.random();
        if(rand <DROP_RATE)
            return;

        Map<String,Object> root = new HashMap<>();
        root.put("station_id",        station_id);
        root.put("s_no",              s_no);
        root.put("battery_status",    battery_status);
        root.put("status_timestamp",  status_timestamp);

        Map<String,Integer> weather = new HashMap<>();
        weather.put("humidity",    humidity);
        weather.put("temperature", temperature);
        weather.put("wind_speed",  wind_speed);
        root.put("weather", weather);

        try {
            String json = MAPPER.writeValueAsString(root);
            System.out.println(json);

            // Send to same Kafka partition by using same key
            ProducerRecord<String, String> record = new ProducerRecord<>("weather-status", "weather", json);
            producer.send(record);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            final int index = i;

            Thread thread = new Thread(() -> {
                long id = index + 1; // default station ID fallback
                String env = System.getenv("STATION_ID_" + index);
                if (env != null) {
                    try {
                        id = Long.parseLong(env);
                    } catch (NumberFormatException ignored) {}
                }

                WeatherStation ws = new WeatherStation();
                ws.initProducer();
                ws.setStation_id(id);

                while (true) {
                    ws.sendMessage();
                    try {
                        Thread.sleep(1_000); // 1 second
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // reset interrupt flag
                        break;
                    }
                }
            });

            thread.start();
        }
    }
}
