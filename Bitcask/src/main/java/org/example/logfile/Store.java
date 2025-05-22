package org.example.logfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
Store: class that has two file pointers one for read and one for write
 */
public class Store {
    RandomAccessFile writer;
    RandomAccessFile reader;
    long currentWriterOffset;
    String filePath;

    /**
    Store constructor: creates an instance of Store from the filePath.
    It creates 2 file pointers: one for writing and other for reading.
     */
    public Store(String filePath){
        try {
            this.writer = new RandomAccessFile(filePath, "rws");
            this.reader = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.currentWriterOffset = 0;
        this.filePath = filePath;
    }

    /**
    append: appends byte[] at the currentWriteOffset and return the offset
     */
    long append(byte[] bytes) {
        long offset = this.currentWriterOffset;
        try {
            this.writer.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        update the current writer offset
        this.currentWriterOffset += bytes.length;

        return offset;
    }

    /**
    read: reads byte[] from some given offset with known size of readable bytes
     */
    byte[] read(long offset, int size) {
        byte[] bytes = null;
        try {
            this.reader.seek(offset);
            bytes = new byte[size];
            this.reader.read(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return bytes;
    }

    /**
    readFull: reads all the file and return byte[]
     */
    byte[] readFull() {
        try {
            return Files.readAllBytes(Path.of(this.filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    sizeInBytes: return size of the file which is the currentWriterOffset
     */
    long sizeInBytes () {
        return this.currentWriterOffset;
    }

    /**
    sync: manually sync write to the disk to ensures that
    all the disk blocks (or pages) at the Kernel page cache are flushed to the disk
    as can be replaced by setting writer to 'rws' mode
     */
    void sync() {
        try {
            this.writer.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    stopWriter: close the writer file pointer
    As this method called when the file segment exceeds its maximum size
     */
    void stopWriter() {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    remove: removes the file
     */
    void remove() {
        File file = new File(this.filePath);
        if (file.delete()) {
            System.out.println("File deleted successfully.");
        } else {
            System.out.println("Failed to delete the file.");
        }
    }
}
