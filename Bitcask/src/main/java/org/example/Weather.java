package org.example;

import org.example.config.Serializable;

import java.io.*;

public class Weather implements Serializable {
    private int humidity;
    private int temperature;
    private int windSpeed;

    public Weather(int humidity, int temperature, int windSpeed) {
        this.humidity = humidity;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(humidity);
            dos.writeInt(temperature);
            dos.writeInt(windSpeed);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Weather deserialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            int humidity = dis.readInt();
            int temperature = dis.readInt();
            int windSpeed = dis.readInt();
            return new Weather(humidity, temperature, windSpeed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Weather{" +
                "humidity=" + humidity +
                ", temperature=" + temperature +
                ", windSpeed=" + windSpeed +
                '}';
    }
}
