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
import org.example.merge.Worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

public class Main {
    private static final Random random = new Random();

    public static BatteryStatus getRandomBatteryStatus() {
        BatteryStatus[] values = BatteryStatus.values();
        int index = random.nextInt(values.length);
        return values[index];
    }

    private static void printSegments(KVStore<TestKey> kvStore, Function<byte[], TestKey> keyMapper) {
        // Active segment
        List<MappedStoredEntry<TestKey>> activeEntries =
                kvStore.getSegments().readFullSegment(kvStore.getSegments().getActiveSegment().getFileId(), keyMapper);
        System.out.println("Active segment entries:");
        for (MappedStoredEntry<TestKey> e : activeEntries) {
            System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
        }

        // Inactive segments
        for (Map.Entry<Long, Segment<TestKey>> entry : kvStore.getSegments().allInactiveSegments().entrySet()) {
            long fileId = entry.getKey();
            List<MappedStoredEntry<TestKey>> entries = kvStore.getSegments().readFullSegment(fileId, keyMapper);
            System.out.println("Inactive segment fileId=" + fileId + " entries:");
            for (MappedStoredEntry<TestKey> e : entries) {
                System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
            }
        }
    }

    private static void printKeyDirectory(KVStore<TestKey> kvStore) {
        System.out.println("KeyDirectory:");
        for (TestKey key : kvStore.getKeyDirectory().getEntryByKey().keySet()) {
            byte[] value = kvStore.get(key);
            System.out.println("Key: " + key + ", Message: " + Message.deserialize(value));
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {


        String path = "E:\\CSED\\year 3\\term 6\\DDIA\\labs\\project\\data";
//        File file = new File(path);
//        if (file.delete()) {
//            System.out.println("File deleted successfully.");
//        } else {
//            System.out.println("Failed to delete the file.");
//        }
        Path tempDir = Files.createDirectory(Path.of(path));
        String directory = path;
        System.out.println("Using directory: " + directory);

        long maxSegmentSizeBytes = 1024; // 1KB
        int keyDirectoryCapacity = 50;
        Clock clock = new SystemClock();

        Function<byte[], TestKey> keyMapper = TestKey::deserialize;
        Duration duration = Duration.ofSeconds(1);
        MergeConfig<TestKey> mergeConfig = new MergeConfig<>(duration, keyMapper);

        Config<TestKey> config = new Config<>(
                directory,
                maxSegmentSizeBytes,
                keyDirectoryCapacity,
                mergeConfig,
                clock);


        KVStore<TestKey> kvStore = new KVStore<>(config);
        Worker<TestKey> worker = new Worker<>(kvStore, config);

        long[] stationsId = new long[5];
        for (int i = 0; i < 5; i++) {
            stationsId[i] = i + 1;
        }

//         Create and append messages
        for (int i = 0; i < 40; i++) {
            Weather weather = new Weather(
                    50 + i,
                    20 + i,
                    10 + i);
            Message msg = new Message(
                    stationsId[i % 5],
                    10,
                    getRandomBatteryStatus().name(),
                    System.currentTimeMillis(),
                    weather);

            // Serialize message to bytes
            byte[] valueBytes = msg.serialize();

            kvStore.put(new TestKey(Long.toString(stationsId[i % 5])), valueBytes);
        }

        System.out.println("Before merge:");
        printSegments(kvStore, keyMapper);
        printKeyDirectory(kvStore);

        Thread.sleep(3000); // 3 seconds > merge interval
        worker.stop();

        System.out.println("\nAfter merge:");
        printSegments(kvStore, keyMapper);
        printKeyDirectory(kvStore);
    }
}