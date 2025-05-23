package org.example.logfile.entry;

public class DecodedEntry {
    private final StoredEntry entry;
    private final int offset;

    public DecodedEntry(StoredEntry entry, int offset) {
        this.entry = entry;
        this.offset = offset;
    }

    public StoredEntry getEntry() {
        return entry;
    }

    public int getOffset() {
        return offset;
    }
}
