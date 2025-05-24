package org.example.logfile.segment;

public class WriteBackResponse<K> {
    private final K key;
    private final AppendEntryResponse appendEntryResponse;

    public WriteBackResponse(K key, AppendEntryResponse appendEntryResponse) {
        this.key = key;
        this.appendEntryResponse = appendEntryResponse;
    }

    public K getKey() {
        return key;
    }

    public AppendEntryResponse getAppendEntryResponse() {
        return appendEntryResponse;
    }
}
