package org.example.kv;

import org.example.logfile.segment.AppendEntryResponse;

/**
 * EntryPointer: represents the value of the KeyDirectory as
 * it is pointing to the stored entry into segment file
 */
public class EntryPointer {
    private final long fileId;
    private final long offset;
    private final int entryLength;

    public EntryPointer(long fileId, long offset, int entryLength) {
        this.fileId = fileId;
        this.offset = offset;
        this.entryLength = entryLength;
    }

    public EntryPointer(AppendEntryResponse response) {
        this.fileId = response.getFileId();
        this.offset = response.getOffset();
        this.entryLength = response.getEntryLength();
    }

    public long getFileId() {
        return fileId;
    }

    public long getOffset() {
        return offset;
    }

    public int getEntryLength() {
        return entryLength;
    }
}
