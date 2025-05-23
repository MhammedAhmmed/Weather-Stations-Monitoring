package org.example.logfile.entry;

import org.example.config.BitCaskKey;

public class MappedStoredEntry <K extends BitCaskKey> {
    private K key;
    private byte[] value;
    private long timeStamp;
    private int keyOffset;
    private int entryLength;

    public MappedStoredEntry(K key, byte[] value, long timeStamp, int keyOffset, int entryLength) {
        this.key = key;
        this.value = value;
        this.timeStamp = timeStamp;
        this.keyOffset = keyOffset;
        this.entryLength = entryLength;
    }

    public K getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getKeyOffset() {
        return keyOffset;
    }

    public int getEntryLength() {
        return entryLength;
    }
}
