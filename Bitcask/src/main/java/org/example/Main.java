package org.example;

import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.Config;
import org.example.config.MergeConfig;
import org.example.config.TestKey;
import org.example.kv.EntryPointer;
import org.example.kv.KVStore;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.segment.AppendEntryResponse;
import org.example.logfile.segment.Segment;
import org.example.logfile.segment.Segments;

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

        String path = "E:\\CSED\\year 3\\term 6\\DDIA\\labs\\project\\data";
//        Path tempDir = Files.createDirectory(Path.of(path));
        String directory = path;
        System.out.println("Using directory: " + directory);

        long maxSegmentSizeBytes = 1024; // 1KB
        int keyDirectoryCapacity = 50;
        Clock clock = new SystemClock();

        Function<byte[], TestKey> keyMapper = TestKey::deserialize;
        MergeConfig<TestKey> mergeConfig = new MergeConfig<>(keyMapper);

        Config<TestKey> config = new Config<>(
                directory,
                maxSegmentSizeBytes,
                keyDirectoryCapacity,
                mergeConfig,
                clock);

        KVStore<TestKey> kvStore = new KVStore<>(config);


        long[] stationsId = new long[5];
        for (int i = 0; i < 5; i++) {
            stationsId[i] = i + 1;
        }

//        // Create and append 5 messages
//        for (int i = 0; i < 20; i++) {
//            Weather weather = new Weather(
//                    50 + i,
//                    20 + i,
//                    10 + i);
//            Message msg = new Message(
//                    stationsId[i % 5],
//                    10,
//                    getRandomBatteryStatus().name(),
//                    System.currentTimeMillis(),
//                    weather);
//
//            // Serialize message to bytes
//            byte[] valueBytes = msg.serialize();
//
//            kvStore.put(new TestKey(Long.toString(stationsId[i % 5])), valueBytes);
//        }

//        // test get from kvStore
//        byte[] bytes = kvStore.get(new TestKey(Long.toString(2)));
//        System.out.println(Message.deserialize(bytes));


//        Function<byte[], TestKey> keyMapper = TestKey::deserialize;
        // Read active segment entries
        List<MappedStoredEntry<TestKey>> activeEntries = kvStore.getSegments().readFullSegment(
                kvStore.getSegments().getActiveSegment().getFileId(), keyMapper);
        System.out.println("Size: " + activeEntries.size());
        System.out.println("Active segment entries:");
        for (MappedStoredEntry<TestKey> e : activeEntries) {
            Message msg = Message.deserialize(e.getValue());  // Deserialize value bytes
            System.out.println("Key: " + e.getKey() + ", Message: " + msg);
        }

        // Read inactive segments entries
        for (Map.Entry<Long, Segment<TestKey>> entry : kvStore.getSegments().allInactiveSegments().entrySet()) {
            long fileId = entry.getKey();
            List<MappedStoredEntry<TestKey>> entries = kvStore.getSegments().readFullSegment(fileId, keyMapper);
            System.out.println("Inactive segment fileId=" + fileId + " entries:");
            for (MappedStoredEntry<TestKey> e : entries) {
                Message msg = Message.deserialize(e.getValue());
                System.out.println("Key: " + e.getKey() + ", Message: " + msg);
            }
        }

        System.out.println("KeyDirectory: ");
        //Read KVStore
        for (TestKey key: kvStore.getKeyDirectory().getEntryByKey().keySet()) {
            byte[] bytes = kvStore.get(key);
            System.out.println("Key: " + key.toString() + " Message: " + Message.deserialize(bytes));
        }
    }
}