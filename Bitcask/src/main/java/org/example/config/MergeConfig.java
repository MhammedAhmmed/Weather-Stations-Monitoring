package org.example.config;

import java.time.Duration;
import java.util.function.Function;

/**
 * MergeConfig: configuration of the merge segment files
 * @param <K>
 */
public class MergeConfig <K extends BitCaskKey> {
    private int totalSegmentsToRead;
    private boolean shouldReadAllSegments;
    private Function<byte[], K> keyMapper;
    private Duration runMergeEvery;

    /**
     * instantiate new MergeConfig with number of segments to read and dynamic scheduling
     * @param totalSegmentsToRead
     * @param keyMapper
     */
    // todo: check for Duration.ofMinutes method
    public MergeConfig(int totalSegmentsToRead, Function<byte[], K> keyMapper) {
        this.totalSegmentsToRead = totalSegmentsToRead;
        this.shouldReadAllSegments = false;
        this.keyMapper = keyMapper;
        this.runMergeEvery = Duration.ofMinutes(5);
    }

    /**
     * instantiate new MergeConfig with number of segments to read and given scheduling
     * @param totalSegmentsToRead
     * @param keyMapper
     * @param runMergeEvery
     */
    public MergeConfig(int totalSegmentsToRead, Function<byte[], K> keyMapper, Duration runMergeEvery) {
        this.totalSegmentsToRead = totalSegmentsToRead;
        this.shouldReadAllSegments = false;
        this.keyMapper = keyMapper;
        this.runMergeEvery = runMergeEvery;
    }

    /**
     * instantiate new MergeConfig with read all segments with all segments to read and dynamic scheduling
     * @param keyMapper
     */
    public MergeConfig(Function<byte[], K> keyMapper) {
        this.shouldReadAllSegments = true;
        this.keyMapper = keyMapper;
        this.runMergeEvery = Duration.ofMinutes(5);;
    }

    /**
     * instantiate new MergeConfig with read all segments with all segments to read and fixed scheduling
     * @param runMergeEvery
     * @param keyMapper
     */
    public MergeConfig(Duration runMergeEvery, Function<byte[], K> keyMapper) {
        this.shouldReadAllSegments = true;
        this.runMergeEvery = runMergeEvery;
        this.keyMapper = keyMapper;
    }

    public int getTotalSegmentsToRead() {
        return totalSegmentsToRead;
    }

    public boolean isShouldReadAllSegments() {
        return shouldReadAllSegments;
    }

    public Function<byte[], K> getKeyMapper() {
        return keyMapper;
    }

    public Duration getRunMergeEvery() {
        return runMergeEvery;
    }
}
