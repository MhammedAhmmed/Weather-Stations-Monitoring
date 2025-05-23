package org.example.logfile.entry;

import org.example.config.BitCaskKey;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.example.config.EntryEncodingConstants.*;
import static org.example.config.EntryEncodingConstants.RESERVED_VALUE_SIZE;


/**
 * EntryCodec: represents the main logic for encoding and decoding
 * for the entries in the file
 * @param <K>
 */
public class EntryCodec<K extends BitCaskKey>{
    /**
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ timestamp │ key_size │ value_size │ key │ value │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * encode: encodes the entry into bytes as
     * timestamp:8_bytes, key_size:4_bytes, value_size:4_bytes and
     * key, value based on their actual size that is stored in key_size and value_size
     * @return
     */
    public byte[] encode(Entry<K> entry) {
        byte[] serializedKey = entry.getKey().serialize();;
        byte[] value = entry.getValue();
        int keySize = serializedKey.length;
        int valueSize = entry.getValue().length;

        int encodedSize =
                RESERVED_TIMESTAMP_SIZE +
                        RESERVED_KEY_SIZE +
                        RESERVED_VALUE_SIZE +
                        keySize +
                        valueSize;
        ByteBuffer encoded = ByteBuffer.allocate(encodedSize);
        encoded.order(ByteOrder.LITTLE_ENDIAN);

        // 1. Timestamp
        long timestamp = entry.getTimeStamp();
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

    /**
     * decode: decodes the entry from offset 0
     * @param content
     * @return
     */
    public StoredEntry decode(byte[] content) {
        int offset = 0;
        DecodedEntry decodedEntry = decodeFrom(content, offset);
        return decodedEntry.getEntry();
    }

    /**
     * decodeMulti: performs multiple decode operations and returns an array of MappedStoredEntry
     * used when a segment file needs to be read completely.
     * This happens during reload and merge operations
     * @param content
     * @return
     */
    public List<MappedStoredEntry<K>> decodeMulti(byte[] content, Function<byte[], K> keyMapper) {
        int contentLength = content.length;
        int offset = 0;

        List<MappedStoredEntry<K>> mappedStoredEntries = new ArrayList<>();

        while (offset < contentLength) {
            DecodedEntry decodedEntry = decodeFrom(content, offset);
            MappedStoredEntry<K> mappedStoredEntry =
                    new MappedStoredEntry<>(
                            keyMapper.apply(decodedEntry.getEntry().getKey()),
                            decodedEntry.getEntry().getValue(),
                            decodedEntry.getEntry().getTimeStamp(),
                            offset,
                            decodedEntry.getOffset()
                            );
            mappedStoredEntries.add(mappedStoredEntry);
            offset += decodedEntry.getOffset();
        }

        return mappedStoredEntries;
    }
    

    /**
     * ┌───────────┬──────────┬────────────┬─────┬───────┐
     * │ timestamp │ key_size │ value_size │ key │ value │
     * └───────────┴──────────┴────────────┴─────┴───────┘
     * decodeFrom: decodes from given offset as
     * the code reads the first 8_bytes to get the timestamp,
     * next 4_bytes to get the key size, next 4_bytes to get the value size
     * Reading further from the offset to the offset+keySize return the actual key,
     * followed by next read from offset to offset+valueSize which returns the actual value.
     * @param content
     * @param offset
     * @return
     */
    public DecodedEntry decodeFrom(byte[] content, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Move buffer position to offset
        buffer.position(offset);

        // 1. Read timestamp
        long timeStamp = buffer.getLong();
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

        StoredEntry storedEntry = new StoredEntry(serializedKey, value, timeStamp);
        DecodedEntry decodedEntry = new DecodedEntry(storedEntry, offset);

        return decodedEntry;
    }
}
