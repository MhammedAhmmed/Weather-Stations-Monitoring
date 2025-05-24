package org.example.logfile.segment;

/**
 * AppendedEntryResponse: response from appending entry into segment
 */
public class AppendEntryResponse {
    private long fileId;
    private long offset;
    private int entryLength;

    public AppendEntryResponse(long fileId, long offset, int entryLength) {
        this.fileId = fileId;
        this.offset = offset;
        this.entryLength = entryLength;
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
