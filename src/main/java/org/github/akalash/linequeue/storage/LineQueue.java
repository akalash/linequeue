package org.github.akalash.linequeue.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.akalash.linequeue.network.PortListenWorker;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Thread-safe FIFO storage of line.
 */
public class LineQueue {
    private static final Logger log = LogManager.getLogger(PortListenWorker.class);

    /** Byte buffer size for write/read operation. */
    private static final int MAX_BUFFER_SIZE = 1024;

    /** Path to file which this queue should be dumped to/restored from. */
    private final String dumpFilePath;

    /** Inner sequential number of last stored entry. */
    private final AtomicLong lastStoredId = new AtomicLong();

    /** Inner sequential number of last read entry. */
    private final AtomicLong lastReadId = new AtomicLong();

    private final ConcurrentHashMap<Long, String> storage = new ConcurrentHashMap<>();

    public LineQueue(String dumpFilePath) {
        this.dumpFilePath = dumpFilePath;
    }

    /** Adding new value to queue. */
    public void add(String value) {
        storage.put(lastStoredId.incrementAndGet(), value);
    }

    /**
     * Poll first {@code count} lines from this queue.
     *
     * @param count Number of lines which should be polled.
     * @return First requested lines or {@code null} if requested count is incorrect.
     */
    public List<String> poll(int count) {
        long firstId;
        do {
            firstId = this.lastReadId.get();

            if (firstId + count > lastStoredId.get())
                return null;
        }
        while (!this.lastReadId.compareAndSet(firstId, firstId + count));

        List<String> res = new ArrayList<>(count);
        for (long i = firstId + 1; i <= firstId + count; i++) {
            String remove = null;

            while (remove == null)
                remove = storage.remove(i);

            res.add(remove);
        }

        return res;
    }

    /** Dumping this queue to the configured file. */
    public void dump() {
        ByteBuffer writeBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);

        try (FileChannel ch = FileChannel.open(Paths.get(dumpFilePath), WRITE, CREATE)) {
            for (long i = lastReadId.get() + 1; i <= lastStoredId.get(); i++) {
                byte[] bytes = storage.get(i).getBytes();
                writeBuffer.clear();

                if (writeBuffer.remaining() < bytes.length + Integer.BYTES)
                    writeBuffer = ByteBuffer.allocate(bytes.length + Integer.BYTES);
                else
                    writeBuffer.limit(bytes.length + Integer.BYTES);

                writeBuffer.putInt(bytes.length);
                writeBuffer.put(bytes);

                writeBuffer.flip();

                while (writeBuffer.hasRemaining())
                    ch.write(writeBuffer);
            }

            ch.force(true);
        }
        catch (IOException e) {
            log.error("Something was going wrong during the dump :: ", e);
        }
    }

    /** Restoring from the dump if it exists. */
    public boolean restore() {
        Path path = Paths.get(dumpFilePath);

        if (!path.toFile().exists())
            return true;

        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        try (FileChannel ch = FileChannel.open(path, READ)) {
            while (true) {
                ch.read(readBuffer);

                readBuffer.flip();

                if (!readBuffer.hasRemaining())
                    break;

                int entrySize = readBuffer.getInt();

                byte[] value = new byte[entrySize];

                int i = 0;
                do {
                    for (; i < entrySize && readBuffer.hasRemaining(); i++)
                        value[i] = readBuffer.get();

                    //If entry was too big for one buffer, try to read more data until whole entry wouldn't be read.
                    if (i < entrySize) {
                        readBuffer.clear();

                        ch.read(readBuffer);

                        readBuffer.flip();

                        if (!readBuffer.hasRemaining()) {
                            log.error(
                                "Not enough data. Perhaps the dump was corrupted. " +
                                    "Expected entry size = {}, but read only = {}", entrySize, i
                            );

                            return false;
                        }
                    }
                }
                while (i < entrySize);

                if (readBuffer.hasRemaining())
                    readBuffer.compact();
                else
                    readBuffer.clear();

                add(new String(value));
            }

            return true;
        }
        catch (IOException e) {
            log.error("Something was going wrong during the restore :: ", e);

            return false;
        }
        finally {
            try {
                Files.delete(path);//TODO: should be do so?
            }
            catch (IOException exception) {
                log.error("Deletion dump file filed :: ", exception);
            }
        }
    }
}
