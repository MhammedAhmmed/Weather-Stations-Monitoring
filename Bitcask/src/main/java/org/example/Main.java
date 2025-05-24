package org.example;

import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.TestKey;
import org.example.logfile.Store;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.entry.StoredEntry;
import org.example.logfile.segment.AppendEntryResponse;
import org.example.logfile.segment.Segment;
import org.example.logfile.segment.Segments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class Main {
    private static final Random random = new Random();

    public static BatteryStatus getRandomBatteryStatus() {
        BatteryStatus[] values = BatteryStatus.values();
        int index = random.nextInt(values.length);
        return values[index];
    }

    public static void main(String[] args) throws IOException {
        Path tempDir = Files.createTempDirectory("bitcask_test");
        String directory = tempDir.toString();
        System.out.println("Using directory: " + directory);

        Clock clock = new SystemClock();
        long maxSegmentSizeBytes = 1024 * 1024; // 1MB

        // Step 1: Create Segments instance
        Segments<TestKey> segments = new Segments<>(directory, maxSegmentSizeBytes, clock);
        System.out.println("Number of inActive segments: " + segments.getInactiveSegments().size());

        // Create and append 5 messages
        for (long i = 1; i <= 5; i++) {
            Weather weather = new Weather(
                    50 + (int)i,
                    20 + (int)i,
                    10 + (int)i);
            Message msg = new Message(
                    i,
                    1,
                    getRandomBatteryStatus().name(),
                    System.currentTimeMillis(),
                    weather);

            // Serialize message to bytes
            byte[] valueBytes = msg.serialize();

            AppendEntryResponse response = segments.append(
                    new TestKey(Long.toString(msg.getStationId())), valueBytes);

            System.out.println("Appended message key=" + i + " at offset=" + response.getOffset());
        }

        Function<byte[], TestKey> keyMapper = TestKey::deserialize;
        // Read active segment entries
        List<MappedStoredEntry<TestKey>> activeEntries = segments.readFullSegment(segments.getActiveSegment().getFileId(), keyMapper);
        System.out.println("Active segment entries:");
        for (MappedStoredEntry<TestKey> e : activeEntries) {
            Message msg = Message.deserialize(e.getValue());  // Deserialize value bytes
            System.out.println("Key: " + e.getKey() + ", Message: " + msg);
        }

        // Read inactive segments entries
        for (Map.Entry<Long, Segment<TestKey>> entry : segments.allInactiveSegments().entrySet()) {
            long fileId = entry.getKey();
            List<MappedStoredEntry<TestKey>> entries = segments.readFullSegment(fileId, keyMapper);
            System.out.println("Inactive segment fileId=" + fileId + " entries:");
            for (MappedStoredEntry<TestKey> e : entries) {
                Message msg = Message.deserialize(e.getValue());
                System.out.println("Key: " + e.getKey() + ", Message: " + msg);
            }
        }
    }
}