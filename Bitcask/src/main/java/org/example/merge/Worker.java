package org.example.merge;

import org.example.config.BitCaskKey;
import org.example.config.Config;
import org.example.config.Pair;
import org.example.kv.KVStore;
import org.example.logfile.entry.MappedStoredEntry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Worker: encapsulates KVStore and MergeConfig.
 * Worker is an abstraction inside merge package that
 * performs merge of inactive segment files every fixed duration
 * @param <K>
 */
public class Worker <K extends BitCaskKey> {
    private KVStore<K> kvStore;
    private Config<K> config;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    /**
     * Worker constructor: creates a new instance of worker
     * and starts worker
     * @param kvStore
     * @param config
     */
    public Worker(KVStore<K> kvStore, Config<K> config) {
        this.kvStore = kvStore;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private void start() {
        Duration interval = config.getMergeConfig().getRunMergeEvery();
        scheduledTask = scheduler.scheduleAtFixedRate(
                this::beginMerge,
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * beginMerge: starts merges in segment files with the last new value for each key as if
     * Segment file F1 has a stored entry with (timestamp:t1, key_size, value_size, key:K1, value:V1)
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ T1        │ key_size │ value_size │ K1  │ V1    │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * and
     * Segment file F2 has a stored entry with (timestamp:t2, key_size, value_size, key:K1, value:V2)
     * which is the same key but with updated value
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ T2        │ key_size │ value_size │ K1  │ V2    │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * so after merging should update state of KeyDirectory, which will already
     * contains K1 pointing to the offset of K1 in the segment file F2.
     * so merge process starts, and it reads the contents of F1 and F2 and performs a merge.
     * The merge writes the key K1 with its new value V2 and timestamp T2 in a new file F3,
     * and deletes files F1 and F2.
     * Segment file F3
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ T2        │ key_size │ value_size │ K1  │ V2    │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * but after The moment merge process is done the state of Key K1
     * needs to be updated in the KeyDirectory to point to the new offset in the new merged file.
     */
    public void beginMerge() {
        try {
            Pair<long[], List<List<MappedStoredEntry<K>>>> listPair = null;

            if (this.config.getMergeConfig().isShouldReadAllSegments()) {
                listPair = this.kvStore.readAllInactiveSegments(this.config.getMergeConfig().getKeyMapper());
            } else {
                listPair = this.kvStore.readInactiveSegments(
                        this.config.getMergeConfig().getTotalSegmentsToRead(),
                        this.config.getMergeConfig().getKeyMapper());
            }

            // if number of segments >= 2 takeAll the segments of the first file
            // then apply mergeWith that segments
            if (listPair.second.size() >= 2) {
                MergeState<K> mergeState = new MergeState<>();
                mergeState.takeAll(listPair.second.getFirst());

                for (int i = 1; i < listPair.second.size(); i++) {
                    mergeState.mergeWith(listPair.second.get(i));
                }
                // after finish merge write back the merged segment file
                // into new inactive segment file

                this.kvStore.writeBack(listPair.first, mergeState.getValueByKey());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * stop: closes the quit channel which is used to signal the merge goroutine to stop
     */
    public void stop() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
        scheduler.shutdownNow();
    }
}
