package org.example.merge;

import org.example.config.BitCaskKey;
import org.example.logfile.entry.MappedStoredEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MergedState: encapsulates key and its entry from inactive segment files
 * @param <K>
 */
public class MergeState <K extends BitCaskKey> {
    private Map<K, MappedStoredEntry<K>> valueByKey;

    public MergeState() {
        this.valueByKey = new HashMap<>();
    }

    public Map<K, MappedStoredEntry<K>> getValueByKey() {
        return valueByKey;
    }

    /**
     * merge performs a merge operation between 2 sets of entries
     * @param entries
     * @param otherEntries
     */
    public void merge(
            List<MappedStoredEntry<K>> entries,
            List<MappedStoredEntry<K>> otherEntries) {

        takeAll(entries);
        mergeWith(otherEntries);
    }

    /**
     * takeAll: accepts all the entries as is and dumps these entries in the hashmap
     * @param mappedStoredEntries
     */
    public void takeAll(List<MappedStoredEntry<K>> mappedStoredEntries) {
        for (MappedStoredEntry<K> entry: mappedStoredEntries) {
            this.valueByKey.put(entry.getKey(), entry);
        }
    }

    /**
     * merge:performs a merge operation with the new set of entries based on timestamp.
     * The value of key with the latest timestamp is retained
     * @param mappedStoredEntries
     */
    public void mergeWith(List<MappedStoredEntry<K>> mappedStoredEntries) {
        for (MappedStoredEntry<K> newEntry: mappedStoredEntries) {
            MappedStoredEntry<K> existing = this.valueByKey.get(newEntry.getKey());
            // if the key not exist update the state of valueByKey,
            // else may update the state if the value is updated
            if (!this.valueByKey.containsKey(newEntry.getKey())) {
                this.valueByKey.put(newEntry.getKey(), newEntry);
            } else {
                mayBeUpdate(existing, newEntry);
            }
        }
    }

    /**
     * mayBeUpdate: updates the value of existing key based on timestamp
     * @param existingEntry
     * @param newEntry
     */
    public void mayBeUpdate(
            MappedStoredEntry<K> existingEntry,
            MappedStoredEntry<K> newEntry) {
        if (newEntry.getTimeStamp() > existingEntry.getTimeStamp()) {
            this.valueByKey.put(existingEntry.getKey(), newEntry);
        }
    }
}
