package org.example.config;

import org.example.clock.Clock;
import org.example.clock.SystemClock;

/**
 * Config: configuration of the bitcask system
 */
public class Config <K extends BitCaskKey>{
    private final String directory;
    private final long maxSegmentSizeBytes;
    private final int keyDirectoryCapacity;
    private final MergeConfig<K> mergeConfig;
    private final Clock clock;

    public Config(String directory, long maxSegmentSizeBytes, int keyDirectoryCapacity, MergeConfig<K> mergeConfig) {
        this.directory = directory;
        this.maxSegmentSizeBytes = maxSegmentSizeBytes;
        this.keyDirectoryCapacity = keyDirectoryCapacity;
        this.mergeConfig = mergeConfig;
        this.clock = new SystemClock();
    }

    public Config(String directory, long maxSegmentSizeBytes, int keyDirectoryCapacity, MergeConfig<K> mergeConfig, Clock clock) {
        this.directory = directory;
        this.maxSegmentSizeBytes = maxSegmentSizeBytes;
        this.keyDirectoryCapacity = keyDirectoryCapacity;
        this.mergeConfig = mergeConfig;
        this.clock = clock;
    }


    public String getDirectory() {
        return directory;
    }

    public long getMaxSegmentSizeBytes() {
        return maxSegmentSizeBytes;
    }

    public int getKeyDirectoryCapacity() {
        return keyDirectoryCapacity;
    }

    public Clock getClock() {
        return clock;
    }

    public MergeConfig<K> getMergeConfig() {
        return mergeConfig;
    }
}
