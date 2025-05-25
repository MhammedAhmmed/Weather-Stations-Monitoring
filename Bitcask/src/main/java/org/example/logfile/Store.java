package org.example.logfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Store: class that has two file pointers one for read and one for write
 */
public class Store {
    RandomAccessFile writer;
    RandomAccessFile reader;
    long currentWriterOffset;
    String filePath;

    public Store() {
    }

    /**
     * Store constructor: creates an instance of Store from the filePath.
     * It creates 2 file pointers: one for writing and other for reading.
     * @param filePath
     */
    public static Store newStore(String filePath){
        Store store = new Store();
        try {
            store.writer = new RandomAccessFile(filePath, "rws");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            store.reader = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        store.currentWriterOffset = 0;
        store.filePath = filePath;

        return store;
    }

    /**
     * reloadStore: creates an instance of Store with only the read file pointer.
     * This operation is executed only during the start-up to reload the state, if any from disk.
     * @param filePath
     * @return
     */
    public static Store reloadStore(String filePath) {
        Store store = new Store();
        store.writer = null;
        try {
            store.reader = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        store.currentWriterOffset = 0;
        store.filePath = filePath;

        return store;
    }

    /**
     * append: appends byte[] at the currentWriteOffset and return the offset
     * @param bytes
     * @return
     */
   public long append(byte[] bytes) {
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
     * read: reads byte[] from some given offset with known size of readable bytes
     * @param offset
     * @param size
     * @return
     */
    public byte[] read(long offset, int size) {
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
     * readFull: reads all the file and return byte[]
     * @return
     */
    public byte[] readFull() {
        try {
            return Files.readAllBytes(Path.of(this.filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * sizeInBytes: return size of the file which is the currentWriterOffset
     * @return
     */
    public long sizeInBytes () {
        return this.currentWriterOffset;
    }

    /**
     * sync: manually sync write to the disk to ensures that
     * all the disk blocks (or pages) at the Kernel page cache are flushed to the disk
     * as can be replaced by setting writer to 'rws' mode
     */
    public void sync() {
        try {
            this.writer.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * stopWriter: close the writer file pointer
     * As this method called when the file segment exceeds its maximum size
     */
    public void stopWrite() {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * close: closes both reader and writer file pointers
     */
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * remove: close the store first then removes the file
     */
    public void remove() {
        close();

        File file = new File(this.filePath);

//        System.out.println("FilePath to delete from store: " + file.getPath());

        if (file.delete()) {
            System.out.println("File deleted successfully.");
        } else {
            System.out.println("Failed to delete the file.");
        }
    }
}
