package org.example;

import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.Config;
import org.example.config.MergeConfig;
import org.example.kv.KVStore;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.segment.Segment;
import org.example.merge.Worker;
import org.example.model.BatteryStatus;
import org.example.model.Key;
import org.example.model.Message;
import org.example.model.Weather;

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

    private static void printSegments(KVStore<Key> kvStore, Function<byte[], Key> keyMapper) {
        // Active segment
        List<MappedStoredEntry<Key>> activeEntries =
                kvStore.getSegments().readFullSegment(kvStore.getSegments().getActiveSegment().getFileId(), keyMapper);
        System.out.println("Active segment entries:");
        for (MappedStoredEntry<Key> e : activeEntries) {
            System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
        }

        // Inactive segments
        for (Map.Entry<Long, Segment<Key>> entry : kvStore.getSegments().allInactiveSegments().entrySet()) {
            long fileId = entry.getKey();
            List<MappedStoredEntry<Key>> entries = kvStore.getSegments().readFullSegment(fileId, keyMapper);
            System.out.println("Inactive segment fileId=" + fileId + " entries:");
            for (MappedStoredEntry<Key> e : entries) {
                System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
            }
        }
    }

    private static void printKeyDirectory(KVStore<Key> kvStore) {
        System.out.println("KeyDirectory:");
        for (Key key : kvStore.getKeyDirectory().getEntryByKey().keySet()) {
            byte[] value = kvStore.get(key);
            System.out.println("Key: " + key + ", Message: " + Message.deserialize(value));
        }
    }


    private static Message createTestMessage(long stationId, int seq) {
        return new Message(
                stationId,
                10,
                getRandomBatteryStatus().name(),
                System.currentTimeMillis(),
                new Weather(50 + seq, 20 + seq, 10 + seq)
        );
    }

    public static void main(String[] args) throws IOException, InterruptedException {


        String path = "E:\\CSED\\year 3\\term 6\\DDIA\\labs\\project\\data";
        // Clean up test directory before starting
        File dir = new File(path);
        if (dir.exists()) {
            Files.walk(Path.of(path))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(Path.of(path));
        String directory = path;
        System.out.println("Using directory: " + directory);

        long maxSegmentSizeBytes = 1024; // 1KB
        int keyDirectoryCapacity = 50;
        Clock clock = new SystemClock();

        Function<byte[], Key> keyMapper = Key::deserialize;
        Duration duration = Duration.ofSeconds(1);
        MergeConfig<Key> mergeConfig = new MergeConfig<>(duration, keyMapper);

        Config<Key> config = new Config<>(
                directory,
                maxSegmentSizeBytes,
                keyDirectoryCapacity,
                mergeConfig,
                clock);


        KVStore<Key> kvStore = new KVStore<>(config);
        Worker<Key> worker = new Worker<>(kvStore, config);

        long[] stationsId = new long[5];
        for (int i = 0; i < 5; i++) {
            stationsId[i] = i + 1;
        }

//         Create and append messages
        for (int i = 0; i < 30; i++) {  // Increased to force segment creation
            Message msg = createTestMessage(stationsId[i % 5], i);
            kvStore.put(new Key(Long.toString(stationsId[i % 5])), msg.serialize());
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