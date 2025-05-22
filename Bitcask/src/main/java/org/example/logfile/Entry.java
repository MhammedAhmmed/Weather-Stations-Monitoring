package org.example.logfile;

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
    K key;
    byte[] value;
    long timeStamp;
    Clock clock;

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

    /**
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ timestamp │ key_size │ value_size │ key │ value │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * encode: encodes the entry into bytes as
     * timestamp:8_bytes, key_size:4_bytes, value_size:4_bytes and
     * key, value based on their actual size that is stored in key_size and value_size
     * @return
     */
    public byte[] encode() {
        byte[] serializedKey = this.key.serialize();
        byte[] value = this.value;
        int keySize = serializedKey.length;
        int valueSize = this.value.length;

        int encodedSize =
                RESERVED_TIMESTAMP_SIZE +
                RESERVED_KEY_SIZE +
                RESERVED_VALUE_SIZE +
                keySize +
                valueSize;
        ByteBuffer encoded = ByteBuffer.allocate(encodedSize);
        encoded.order(ByteOrder.LITTLE_ENDIAN);

        // 1. Timestamp
        long timestamp = this.timeStamp == 0
                ? this.clock.now()
                : this.timeStamp;
        encoded.putLong(timestamp);

        // 2. Key size
        encoded.putInt(keySize);

        // 3. Value size
        encoded.putInt(valueSize);

        // 4. Key bytes
        encoded.put(serializedKey);

        // 5. Value bytes
        encoded.put(value);

        return encoded.array();
    }

    public StoredEntry decodeFrom(byte[] content, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Move buffer position to offset
        buffer.position(offset);

        // 1. Read timestamp
        long timestamp = buffer.getLong();
        offset += RESERVED_TIMESTAMP_SIZE;

        // 2. Read key size
        int keySize = buffer.getInt();
        offset += RESERVED_KEY_SIZE;

        // 3. Read value size
        int valueSize = buffer.getInt();
        offset += RESERVED_VALUE_SIZE;

        // 4. Read serialized key
        byte[] serializedKey = Arrays.copyOfRange(content, offset, offset + keySize);
        offset += keySize;

        // 5. Read value
        byte[] value = Arrays.copyOfRange(content, offset, offset + valueSize);
        offset += valueSize;

        return new StoredEntry(serializedKey, value, timeStamp);
    }
}
