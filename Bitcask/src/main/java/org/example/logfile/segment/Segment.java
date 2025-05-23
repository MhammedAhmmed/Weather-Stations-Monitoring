package org.example.logfile.segment;

import org.example.config.BitCaskKey;
import org.example.logfile.Store;
import org.example.logfile.entry.Entry;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.entry.StoredEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.example.logfile.entry.EntryCodec.*;

/**
 * Segment: is segment file that stores list of entries
 * and accessed by its store
 */
public class Segment <K extends BitCaskKey> {
    long fileId;
    String filePath;
    Store store;

    private final String segmentFilePrefix = "log";
    private final String segmentFileSuffix = "data";

    /**
     * Segment constructor: creates a new segment with its identifier and its store
     * @param fileId
     * @param directory
     */
    public Segment(long fileId, String directory) {
        this.filePath = createSegment(fileId, directory);
        this.fileId = fileId;
        this.store = new Store(this.filePath);
    }

    /**
     * append: appends entry into segment file as
     * 1) encode the entry
     * 2) write encoded entry
     * @param entry
     * @return
     */
    public AppendedEntryResponse append(Entry<K> entry) {
        byte[] encoded = encode(entry);
        long offset = this.store.append(encoded);

        return new AppendedEntryResponse(this.fileId, offset, encoded.length);
    }

    /**
     * read: read performs a read operation from the offset in the segment file.
     * @param offset
     * @param size
     * @return
     */
    public StoredEntry read (long offset, int size) {
        byte[] bytes = this.store.read(offset, size);

        StoredEntry storedEntry = decode(bytes);
        return storedEntry;
    }

    /**
     * readFull: performs a full read of the segment file
     * @param keyMapper
     * @return
     */
    public List<MappedStoredEntry<K>> readFull(Function<byte[], K> keyMapper) {
        byte[] bytes = this.store.readFull();

        List<MappedStoredEntry<K>> mappedStoredEntries = decodeMulti(bytes, keyMapper);
        return mappedStoredEntries;
    }

    /**
     * sizeInBytes: returns size of the segment in bytes
     * @return
     */
    public long sizeInBytes() {
        return this.store.sizeInBytes();
    }

    /**
     * stopWrite: closes writer pointer in segment file when it reaches the threshold
     */
    public void stopWrite() {
        this.store.stopWrite();
    }

    /**
     * remove: removes the segment file as
     * this method is called after merging
     */
    public void remove() {
        this.store.remove();
    }

    /**
     * createSegment: creates a new file
     * @param fileId
     * @param directory
     * @return
     */
    public String createSegment(long fileId, String directory) {
        String filePath = segmentName(fileId, directory);
        try {
            Files.createFile(Path.of(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return filePath;
    }

    /**
     * segmentName: constructs the file path with file name (fileId, 'log', 'data')
     * @param fileId
     * @param directory
     * @return
     */
    public String segmentName(long fileId, String directory) {
        return directory + "/" +  fileId + "_" + segmentFilePrefix + "." + segmentFileSuffix;
    }
}
