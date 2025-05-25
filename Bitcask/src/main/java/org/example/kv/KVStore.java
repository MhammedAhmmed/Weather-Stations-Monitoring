package org.example.kv;

import org.example.config.BitCaskKey;
import org.example.config.Config;
import org.example.config.Pair;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.entry.StoredEntry;
import org.example.logfile.segment.AppendEntryResponse;
import org.example.logfile.segment.Segment;
import org.example.logfile.segment.Segments;
import org.example.logfile.segment.WriteBackResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * KVStore encapsulates append-only log segments and KeyDirectory which is an in-memory hashmap
 * Segments is an abstraction that manages the active and K inactive segments.
 * KVStore also maintains a RWLock that allows an exclusive writer and N readers
 * This is the main controller for bitcask system
 * @param <K>
 */
public class KVStore <K extends BitCaskKey> {
    private Segments<K> segments;
    private KeyDirectory<K> keyDirectory;
    private Lock lock;

    /**
     * KVStore constructor: configures the segments controller and the key directory then
     * reloads the entries from segment files
     * @param config
     */
    // todo: instantiate the lock
    public KVStore(Config<K> config) {
        Segments<K> segments = new Segments<>(config.getDirectory(), config.getMaxSegmentSizeBytes(), config.getClock());
        this.segments = segments;
        this.keyDirectory = new KeyDirectory<>(config.getKeyDirectoryCapacity());

        reload(config);
    }

    public Segments<K> getSegments() {
        return segments;
    }

    public KeyDirectory<K> getKeyDirectory() {
        return keyDirectory;
    }

    public Lock getLock() {
        return lock;
    }

    /**
     * put: writes the key and value into segment file and also add the key and entry pointer
     * in the keyDirectory map
     * @param key
     * @param value
     */
    public void put(K key, byte[] value) {
        this.lock.lock();

        AppendEntryResponse appendEntryResponse = this.segments.append(key, value);

        this.keyDirectory.put(key, new EntryPointer(appendEntryResponse));

        this.lock.unlock();
    }

    /**
     * update: writes the key and the value into the log segment file
     * and updates the entry pointer for the corresponding key into keyDirectory
     * @param key
     * @param value
     */
    public void update(K key, byte[] value) {
        put(key, value);
    }

    /**
     * silentGet: Gets the entry value corresponding to the key. Returns value and true if the value is found,
     * else returns nil and false
     * @param key
     * @return
     */
    public Pair<byte[], Boolean> silentGet(K key) {
        this.lock.lock();

        Pair<EntryPointer, Boolean> entryPointerBooleanPair = this.keyDirectory.get(key);
        if (entryPointerBooleanPair.second) {
            StoredEntry storedEntry = this.segments.read(
                    entryPointerBooleanPair.first.getFileId(),
                    entryPointerBooleanPair.first.getOffset(),
                    entryPointerBooleanPair.first.getEntryLength());

            this.lock.unlock();
            return new Pair<>(storedEntry.getValue(), true);
        }

        this.lock.unlock();
        return new Pair<>(null, false);
    }

    /**
     * get: same as silentGet but returns only the value
     * @param key
     * @return
     */
    public byte[] get(K key) {
        this.lock.lock();

        Pair<EntryPointer, Boolean> entryPointerBooleanPair = this.keyDirectory.get(key);
        if (entryPointerBooleanPair.second) {
            StoredEntry storedEntry = this.segments.read(
                    entryPointerBooleanPair.first.getFileId(),
                    entryPointerBooleanPair.first.getOffset(),
                    entryPointerBooleanPair.first.getEntryLength());
            this.lock.unlock();
            return storedEntry.getValue();
        }
        this.lock.unlock();
        return null;
    }

    /**
     * readInactiveSegments: reads inactive segments identified by `totalSegments`.
     * This operation is performed during merge.
     * @param totalSegments
     * @param keyMapper
     * @return
     */
    public Pair<long[], List<List<MappedStoredEntry<K>>>> readInactiveSegments(
            int totalSegments, Function<byte[], K> keyMapper) {
        this.lock.lock();

        Pair<long[], List<List<MappedStoredEntry<K>>>> pair =
                this.segments.readInactiveSegments(totalSegments, keyMapper);

        this.lock.unlock();

        return pair;
    }

    /**
     * readAllInactiveSegments: reads all the inactive segments. This operation is performed during merge.
     * @param keyMapper
     * @return
     */
    public Pair<long[], List<List<MappedStoredEntry<K>>>> readAllInactiveSegments(
            Function<byte[], K> keyMapper) {
        this.lock.lock();

        Pair<long[], List<List<MappedStoredEntry<K>>>> pair =
                this.segments.readAllInactiveSegments(keyMapper);

        this.lock.unlock();

        return pair;
    }

    /**
     * writeBack:  writes back the changes (merged changes) to new inactive segments.
     * This operation is performed during merge
     * then update the state of keys present in keyDirectory
     * then removes the old segment files
     * @param fileIds
     * @param changes
     */
    public void writeBack(List<Long> fileIds, Map<K, MappedStoredEntry<K>> changes) {
        this.lock.lock();

        List<WriteBackResponse<K>> writeBackResponses = this.segments.writeBack(changes);
        this.keyDirectory.bulkUpdate(writeBackResponses);
        this.segments.remove(fileIds);
        this.lock.unlock();
    }

    /**
     * clearLog: removes all the log files active and in active
     */
    public void clearLog() {
        this.lock.lock();

        this.segments.removeActive();
        this.segments.removeAllInactive();

        this.lock.unlock();
    }

    /**
     * sync: performs a sync of all the active and inactive segments
     */
    public void sync() {
        this.lock.lock();

        this.segments.sync();

        this.lock.unlock();
    }

    /**
     * shutdown: performs a shutdown of the segments which involves setting the active segment to null
     * and removing the entire in-memory representation of the inactive segments
     */
    public void shutdown() {
        this.lock.lock();

        this.segments.shutdown();

        this.lock.unlock();
    }

    /**
     * reload: reloads the entire state during start-up.
     * @param config
     */
    public void reload(Config<K> config) {
        this.lock.lock();

        for (Map.Entry<Long, Segment<K>> entry: this.segments.allInactiveSegments().entrySet()) {
            List<MappedStoredEntry<K>> entries = entry.getValue().readFull(config.getMergeConfig().getKeyMapper());
            this.keyDirectory.reload(entry.getKey(), entries);
        }

        this.lock.unlock();
    }
}
