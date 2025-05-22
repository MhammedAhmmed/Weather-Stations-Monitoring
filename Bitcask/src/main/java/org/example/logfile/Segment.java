package org.example.logfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Segment: is segment file that stores list of entries
 * and accessed by its store
 */
public class Segment {
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
