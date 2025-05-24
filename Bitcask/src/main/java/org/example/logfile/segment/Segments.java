package org.example.logfile.segment;

import org.example.clock.Clock;
import org.example.config.BitCaskKey;
import org.example.config.Pair;
import org.example.logfile.entry.Entry;
import org.example.logfile.entry.MappedStoredEntry;
import org.example.logfile.entry.StoredEntry;
import org.example.logfile.id.TimestampBasedFileIdGenerator;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Segments: control all active and inactive segments and other config
 * @param <K>
 */
public class Segments<K extends BitCaskKey> {
    private Segment<K> activeSegment;
    private Map<Long, Segment<K>> inactiveSegments;
    private TimestampBasedFileIdGenerator fileIdGenerator;
    private Clock clock;
    private long maxSegmentSizeBytes;
    private String directory;


    /**
     * Segments: creates a new segments controller with reload
     * to get all active and inactive files if exist
     * @param directory
     * @param maxSegmentSizeBytes
     * @param clock
     */
    public Segments(String directory, long maxSegmentSizeBytes, Clock clock) {
        this.clock = clock;
        this.directory = directory;
        this.maxSegmentSizeBytes = maxSegmentSizeBytes;

        this.fileIdGenerator = new TimestampBasedFileIdGenerator(clock);
        long fileId = fileIdGenerator.next();
        this.activeSegment = Segment.newSegment(fileId, directory);
        this.inactiveSegments = new HashMap<>();

        reload();
    }

    /**
     * append:  performs an append operation in the active segment file.
     * Before the append operation can be done, the size of the active segment is checked.
     * If its size < the size of segment threshold, the key value pair is appended to the active segment,
     * else the active segment is rolled-over to be in active and write in a new active segment
     * @param key
     * @param value
     * @return
     */
    public AppendEntryResponse append(K key, byte[] value) {
        maybeRolloverActiveSegment();
        return activeSegment.append(new Entry<>(key, value, clock));
    }

    /**
     * read: performs a read operation from the offset in the file segment
     * @param fileId
     * @param offset
     * @param size
     * @return
     */
    public StoredEntry read(long fileId, long offset, int size){
        if (fileId == activeSegment.getFileId()) {
            return activeSegment.read(offset, size);
        }
        Segment<K> segment = inactiveSegments.get(fileId);
        if (segment != null) {
            return segment.read(offset, size);
        }
        throw new IllegalArgumentException("Invalid file id " + fileId);
    }

    /**
     * readInactiveSegments: reads inactive segments identified by `totalSegments`. This operation is performed during merge.
     * @param totalSegments
     * @param keyMapper
     * @return
     */
    public Pair<long[], List<List<MappedStoredEntry<K>>>> readInactiveSegments(int totalSegments, Function<byte[], K> keyMapper){
        List<List<MappedStoredEntry<K>>> contents = new ArrayList<>(totalSegments);
        long[] fileIds = new long[totalSegments];
        int index = 0;

        for (Segment<K> segment : inactiveSegments.values()) {
            if (index >= totalSegments) break;
            List<MappedStoredEntry<K>> entries = segment.readFull(keyMapper);
            contents.add(entries);
            fileIds[index] = segment.getFileId();
            index++;
        }

        return new Pair<>(fileIds, contents);
    }

    /**
     * readAllInactiveSegments: reads inactive segments. This operation is performed during merge.
     * @param keyMapper
     * @return
     */
    public Pair<long[], List<List<MappedStoredEntry<K>>>> readAllInactiveSegments(Function<byte[], K> keyMapper) {
        return readInactiveSegments(inactiveSegments.size(), keyMapper);
    }

    /**
     * writeBack: writes back the changes when merged changes to new in active segments.
     * This operation is performed during merge.
     * @param changes
     * @return
     */
    public List<WriteBackResponse<K>> writeBack(Map<K, MappedStoredEntry<K>> changes) {
        Segment<K> segment = Segment.newSegment(fileIdGenerator.next(), directory);
        inactiveSegments.put(segment.getFileId(), segment);

        List<WriteBackResponse<K>> responses = new ArrayList<>(changes.size());
        for (Map.Entry<K, MappedStoredEntry<K>> entry : changes.entrySet()) {
            AppendEntryResponse response = segment.append(
                    new Entry<>(entry.getKey(), entry.getValue().getValue(), entry.getValue().getTimeStamp(), clock));
            responses.add(new WriteBackResponse<>(entry.getKey(), response));

            Segment<K> newSegment = maybeRolloverSegment(segment);
            if (newSegment != null) {
                inactiveSegments.put(newSegment.getFileId(), newSegment);
                segment = newSegment;
            }
        }
        return responses;
    }

    /**
     * removeActive:  removes the active segment file from disk
     */
    public void removeActive(){
        activeSegment.remove();
    }

    /**
     * removeAllInactive: removes all the inactive segment files from disk
     */
    public void removeAllInactive() {
        for (Segment<K> segment : inactiveSegments.values()) {
            segment.remove();
        }
    }

    /**
     * remove: removes all the inactive files identified by fileIds.
     * This operation is called from WriteBack during merging operation
     * @param fileIds
     */
    public void remove(List<Long> fileIds) {
        for (Long fileId : fileIds) {
            Segment<K> segment = inactiveSegments.remove(fileId);
            if (segment != null) {
                segment.remove();
            }
        }
    }

    /**
     * allInactiveSegments: returns all the inactive segments
     * @return
     */
    public Map<Long, Segment<K>> allInactiveSegments() {
        return Collections.unmodifiableMap(inactiveSegments);
    }

    public void sync() {
        activeSegment.sync();
        for (Segment<K> segment : inactiveSegments.values()) {
            segment.sync();
        }
    }

    /**
     * shutdown: sets the active segment to nil and deletes all the keys from the inactive segments
     */
    public void shutdown() {
        activeSegment = null;
        inactiveSegments.clear();
    }

    /**
     * maybeRolloverActiveSegment: makes the active segment to be in active and make a new active segment
     */
    private void maybeRolloverActiveSegment() {
        Segment<K> newSegment = maybeRolloverSegment(activeSegment);
        if (newSegment != null) {
            inactiveSegments.put(activeSegment.getFileId(), activeSegment);
            activeSegment = newSegment;
        }
    }

    /**
     * maybeRolloverSegment: checks if rollover happened then return the segment that will be written.
     * if null returned then the write operation is happened
     * @param segment
     * @return
     */
    private Segment<K> maybeRolloverSegment(Segment<K> segment) {
        if (segment.sizeInBytes() >= maxSegmentSizeBytes) {
            segment.stopWrite();
            return Segment.newSegment(fileIdGenerator.next(), directory);
        }
        return null;
    }

    /**
     * reload: calls when first initializing the segment control as
     * it loads all entries from inactive segments and add them to the map
     */
    private void reload() {
        // get files in the directory
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(Paths.get(directory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String suffix = Segment.SEGMENT_FILE_PREFIX + "." + Segment.SEGMENT_FILE_SUFFIX;

        for (Path entry : stream) {
            String fileName = entry.getFileName().toString();
            if (fileName.endsWith(suffix)) {
                String[] parts = fileName.split("_");
                long fileId = Long.parseLong(parts[0]);
                if (fileId != activeSegment.getFileId()) {
                    Segment<K> segment = Segment.reloadInactiveSegment(fileId, directory);
                    inactiveSegments.put(fileId, segment);
                }
            }
        }
    }
}
