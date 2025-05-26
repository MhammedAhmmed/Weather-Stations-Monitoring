package org.example.util;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.example.model.Message;
import org.example.model.Weather;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Mapper {
    private static Message convertToMessage(GenericRecord record) {
        long stationId = (Long) record.get("station_id");
        long sNo = (Long) record.get("s_no");
        String batteryStatus = record.get("battery_status").toString();
        long statusTimestamp = (Long) record.get("status_timestamp");
        Weather weather = convertToWeather((GenericRecord) record.get("weather"));

        return new Message(stationId, sNo, batteryStatus, statusTimestamp, weather);
    }

    private static Weather convertToWeather(GenericRecord weatherRecord) {
        int humidity = (Integer) weatherRecord.get("humidity");
        int temperature = (Integer) weatherRecord.get("temperature");
        int windSpeed = (Integer) weatherRecord.get("wind_speed");

        return new Weather(humidity, temperature, windSpeed);
    }

    public static List<Pair<Long, Message>> convertToMessageList(Map<Long, List<GenericRecord>> stationMessages) {
        return stationMessages.entrySet().stream()
                .flatMap(entry ->
                        entry.getValue().stream()
                                .map(record -> Pair.of(entry.getKey(), convertToMessage(record)))
                )
                .collect(Collectors.toList());
    }
}
