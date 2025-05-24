package org.example.config;

import java.util.Arrays;

public class TestKey implements BitCaskKey {
    private final byte[] key;

    public TestKey(String key) {
        this.key = key.getBytes();
    }

    // Add a constructor that takes bytes directly
    public TestKey(byte[] key) {
        this.key = key;
    }

    @Override
    public byte[] serialize() {
        return key;
    }

    // Static method to create TestKey from bytes (deserialization)
    public static TestKey deserialize(byte[] bytes) {
        // Defensive copy for safety
        return new TestKey(Arrays.copyOf(bytes, bytes.length));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TestKey && Arrays.equals(key, ((TestKey) o).key);
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