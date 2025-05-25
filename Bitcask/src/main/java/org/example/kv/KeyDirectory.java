package org.example.kv;

import org.example.config.BitCaskKey;
import org.example.config.Pair;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.segment.WriteBackResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KeyDirectory: is the in-memory storage which maintains a mapping between keys and
 * the position of those keys in the datafiles called segment.
 * @param <K>
 */
public class KeyDirectory <K extends BitCaskKey> {
    private final Map<K, EntryPointer> entryByKey;

    /**
     * KeyDirectory: instantiate entryByKey with initial size
     * @param initialCapacity
     */
    public KeyDirectory(int initialCapacity) {
        this.entryByKey = new HashMap<>(initialCapacity);
    }

    /**
     * reload: reloads the state of the KeyDirectory during start-up.
     * As a part of reloading the state in bitcask model, all the inactive segments are read
     * @param fileId
     * @param entries
     */
    // todo: need to handle read from the hint file after compaction
    public void reload(long fileId, List<MappedStoredEntry<K>> entries) {
        for (MappedStoredEntry<K> entry: entries) {
            this.entryByKey.put(entry.getKey(), new EntryPointer(fileId, entry.getKeyOffset(), entry.getEntryLength()));
        }
    }

    /**
     * put: puts a key and its entry as the value in the KeyDirectory
     * @param key
     * @param value
     */
    public void put(K key, EntryPointer value) {
        this.entryByKey.put(key, value);
    }

    /**
     * bulkUpdate: performs bulk changes to the KeyDirectory state.
     * This method is called during merge and compaction from KeyStore.
     * @param changes
     */
    public void bulkUpdate(List<WriteBackResponse<K>> changes) {
        for (WriteBackResponse<K> change: changes) {
            this.entryByKey.put(change.getKey(), new EntryPointer(change.getAppendEntryResponse()));
        }
    }

    /**
     * get: returns the Entry and a boolean to indicate
     * if the value corresponding to the key is present in the KeyDirectory.
     * as returns nil, false if the value corresponding to the key is not present and
     * returns a pointer to an Entry, true if the value corresponding to the key is present
     * @param key
     * @return
     */
    public Pair<EntryPointer, Boolean> get(K key) {
        EntryPointer entryPointer = this.entryByKey.get(key);
        boolean isExist = this.entryByKey.containsKey(key);
        return new Pair<>(entryPointer, isExist);
    }
}
