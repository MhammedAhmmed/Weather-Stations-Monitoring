package org.example;

import org.example.config.Serializable;

import java.io.*;

public class Message implements Serializable {
    private long stationId;
    private long sNo;
    private String batteryStatus;
    private long statusTimestamp;
    private Weather weather;

    public Message(long stationId, long sNo, String batteryStatus, long statusTimestamp, Weather weather) {
        this.stationId = stationId;
        this.sNo = sNo;
        this.batteryStatus = batteryStatus;
        this.statusTimestamp = statusTimestamp;
        this.weather = weather;
    }

    public long getStationId() {
        return stationId;
    }

    public long getsNo() {
        return sNo;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public long getStatusTimestamp() {
        return statusTimestamp;
    }

    public Weather getWeather() {
        return weather;
    }

    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeLong(stationId);
            dos.writeLong(sNo);

            // Write string length and string bytes
            byte[] batteryBytes = batteryStatus.getBytes("UTF-8");
            dos.writeInt(batteryBytes.length);
            dos.write(batteryBytes);

            dos.writeLong(statusTimestamp);

            // Serialize nested Weather object
            byte[] weatherBytes = weather.serialize();
            dos.writeInt(weatherBytes.length);
            dos.write(weatherBytes);

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Message deserialize(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            long stationId = dis.readLong();
            long sNo = dis.readLong();

            int batteryLength = dis.readInt();
            byte[] batteryBytes = new byte[batteryLength];
            dis.readFully(batteryBytes);
            String batteryStatus = new String(batteryBytes, "UTF-8");

            long statusTimestamp = dis.readLong();

            int weatherLength = dis.readInt();
            byte[] weatherBytes = new byte[weatherLength];
            dis.readFully(weatherBytes);
            Weather weather = Weather.deserialize(weatherBytes);

            return new Message(stationId, sNo, batteryStatus, statusTimestamp, weather);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "stationId=" + stationId +
                ", sNo=" + sNo +
                ", batteryStatus='" + batteryStatus + '\'' +
                ", statusTimestamp=" + statusTimestamp +
                ", weather=" + weather +
                '}';
    }
}
