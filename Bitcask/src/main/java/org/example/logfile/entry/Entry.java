package org.example.logfile.entry;

import org.example.clock.Clock;
import org.example.clock.SystemClock;
import org.example.config.BitCaskKey;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.example.config.EntryEncodingConstants.*;

/**
 * Entry: represents the record that will be written or read into the log file
 */
public class Entry <K extends BitCaskKey> {
    private final K key;
    private final byte[] value;
    private final long timeStamp;
    private final Clock clock;

    /**
     * Entry constructor: creates new entry as it will be written
     * @param key
     * @param value
     */
    public Entry(K key, byte[] value) {
        this.key = key;
        this.value = value;
        this.timeStamp = 0;
        this.clock = new SystemClock();
    }

    /**
     * Entry constructor: creates new entry as it was already written (for read)
     * @param key
     * @param value
     * @param timeStamp
     */
    public Entry(K key, byte[] value, long timeStamp) {
        this.key = key;
        this.value = value;
        this.timeStamp = timeStamp;
        this.clock = new SystemClock();
    }

    public K getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp == 0? clock.now(): timeStamp;
    }
}
