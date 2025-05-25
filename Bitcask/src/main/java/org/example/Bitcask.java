package org.example;

import org.apache.commons.lang3.tuple.Pair;
import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.Config;
import org.example.config.MergeConfig;
import org.example.kv.KVStore;
import org.example.merge.Worker;
import org.example.model.Key;
import org.example.model.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class Bitcask {
    private final KVStore<Key> kvStore;
    private final Worker<Key> worker;

    public Bitcask() throws IOException {
        // Initialize the Bitcask store
        String path = "E:\\CSED\\year 3\\term 6\\DDIA\\labs\\project\\data";

        // Clean up directory if exists (only for demo - remove in production)
        File dir = new File(path);
        if (dir.exists()) {
            Files.walk(Path.of(path))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(Path.of(path));

        // Configuration
        long maxSegmentSizeBytes = 1024 * 1024; // 1MB
        int keyDirectoryCapacity = 1000;
        Clock clock = new SystemClock();
        Duration mergeInterval = Duration.ofSeconds(30);

        // Create key mapper using reflection
        Function<byte[], Key> keyMapper = Key::deserialize;

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

    public void shutdown() {
        worker.stop();
    }
}