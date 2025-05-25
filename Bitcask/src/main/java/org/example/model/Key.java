package org.example.model;

import org.example.config.BitCaskKey;

import java.util.Arrays;

public class Key implements BitCaskKey {
    private final byte[] key;

    public Key(String key) {
        this.key = key.getBytes();
    }

    // Add a constructor that takes bytes directly
    public Key(byte[] key) {
        this.key = key;
    }

    @Override
    public byte[] serialize() {
        return key;
    }

    // Static method to create TestKey from bytes (deserialization)
    public static Key deserialize(byte[] bytes) {
        // Defensive copy for safety
        return new Key(Arrays.copyOf(bytes, bytes.length));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Key && Arrays.equals(key, ((Key) o).key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public String toString() {
        return new String(key);
    }
}