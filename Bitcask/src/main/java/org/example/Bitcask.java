package org.example;

import org.apache.commons.lang3.tuple.Pair;
import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.Config;
import org.example.config.MergeConfig;
import org.example.kv.KVStore;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.segment.Segment;
import org.example.merge.Worker;
import org.example.model.Key;
import org.example.model.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Bitcask {
    private final KVStore<Key> kvStore;
    private final Worker<Key> worker;
    // Create key mapper using reflection
    Function<byte[], Key> keyMapper = Key::deserialize;

    public Bitcask() throws IOException {
        // Initialize the Bitcask store
        String path = "bitcask";

        // Clean up directory if exists (only for demo - remove in production)
        File dir = new File(path);
        if (!dir.exists()) {
            Files.createDirectories(Path.of(path)); // creates parent dirs if needed
        }

        // Configuration
        long maxSegmentSizeBytes = 1024; // 1KB
        int keyDirectoryCapacity = 1000;
        Clock clock = new SystemClock();
        Duration mergeInterval = Duration.ofSeconds(1);


        MergeConfig<Key> mergeConfig = new MergeConfig<>(mergeInterval, keyMapper);
        Config<Key> config = new Config<>(
                path,
                maxSegmentSizeBytes,
                keyDirectoryCapacity,
                mergeConfig,
                clock);

        this.kvStore = new KVStore<>(config);
        this.worker = new Worker<>(kvStore, config);
    }

    public void bulkStore(List<Pair<Long, Message>> messages) {
        for (Pair<Long, Message> pair : messages) {
            kvStore.put(new Key(Long.toString(pair.getKey())), pair.getRight().serialize());
        }
    }

    public void printKeyDirectory() {
        System.out.println("KeyDirectory:");
        for (Key key : kvStore.getKeyDirectory().getEntryByKey().keySet()) {
            byte[] value = kvStore.get(key);
            System.out.println("Key: " + key + ", Message: " + Message.deserialize(value));
        }
    }

    public void printSegments() {
        // Active segment
        List<MappedStoredEntry<Key>> activeEntries =
                kvStore.getSegments().readFullSegment(kvStore.getSegments().getActiveSegment().getFileId(), keyMapper);
        System.out.println("Active segment entries:");
        for (MappedStoredEntry<Key> e : activeEntries) {
            System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
        }

        Map<Long, Segment<Key>> inactiveSegments = new HashMap<>(kvStore.getSegments().allInactiveSegments());
        for (Map.Entry<Long, Segment<Key>> entry : inactiveSegments.entrySet()) {
            long fileId = entry.getKey();
            List<MappedStoredEntry<Key>> entries = kvStore.getSegments().readFullSegment(fileId, keyMapper);
            System.out.println("Inactive segment fileId=" + fileId + " entries:");
            for (MappedStoredEntry<Key> e : entries) {
                System.out.println("Key: " + e.getKey() + ", Message: " + Message.deserialize(e.getValue()));
            }
        }
    }

    public void shutdown() {
        worker.stop();
    }

    public KVStore<Key> getKvStore() {
        return kvStore;
    }
}