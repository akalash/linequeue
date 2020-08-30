package org.github.akalash.linequeue.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Worker which read from/write to socket in non-blocked way.
 */
public class ReadWriteSocketWorker implements Runnable {
    private static final Logger log = LogManager.getLogger(ReadWriteSocketWorker.class);

    /** Changes which should be applied in worker threads. */
    private final ConcurrentLinkedQueue<Runnable> changesCallbacks = new ConcurrentLinkedQueue<>();

    /** List of clients which connections were established to. */
    private final Set<SocketChannel> activeClients = new HashSet<>();

    /** Specific selector only for current worker. */
    private final Selector readWriteSelector;

    /** Read buffer. */
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    /** Factory which create a new connection handler for each new client. */
    private final Function</* Ready for read callback */Runnable, ConnectionFacade> connectionHandlerFactory;

    public ReadWriteSocketWorker(Function<Runnable, ConnectionFacade> factory) throws IOException {
        connectionHandlerFactory = factory;
        readWriteSelector = Selector.open();
    }

    @Override public void run() {
        try {
            handleReadWriteEvents();
        }
        catch (Exception ex) {
            log.error("Error during the handling read-write events", ex);
        }
        finally {
            stop();

            log.info("Read-write to the socket was finished");
        }
    }

    /**
     * Endlessly waiting for read-write socket events.
     *
     * Main targets of this method: Reading data from socket and send it to socket associated handler. Awaiting
     * readiness of handler's data and write it to the socket.
     *
     * @throws IOException If fail.
     */
    public void handleReadWriteEvents() throws IOException {
        log.info("Read-write socket worker started");

        while (!Thread.currentThread().isInterrupted()) {

            Runnable changes;
            while ((changes = changesCallbacks.poll()) != null)
                changes.run();

            int keyCount = readWriteSelector.select(1000);

            if (keyCount == 0)
                continue;

            Iterator<SelectionKey> keyIterator = readWriteSelector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                keyIterator.remove();

                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel)key.channel();

                    readBuffer.clear();

                    int actualRead = client.read(readBuffer);

                    if (actualRead == -1) {
                        closeClient(client);

                        continue;
                    }

                    readBuffer.flip();

                    ((ConnectionFacade)key.attachment()).requestReceived(readBuffer);
                }

                if (key.isWritable()) {
                    SocketChannel client = (SocketChannel)key.channel();
                    ConnectionFacade handler = (ConnectionFacade)key.attachment();

                    ByteBuffer data = handler.nextResponse();

                    if (data != null)
                        client.write(data);
                    else
                        closeClient(client);

                    if (!handler.hasNextResponse()) {
                        if ((key.interestOps() & SelectionKey.OP_WRITE) != 0)
                            key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
                    }
                }
            }
        }
    }

    private void closeClient(SocketChannel client) throws IOException {
        String clientAddress = null;
        try {
            clientAddress = client.getRemoteAddress().toString();
        }
        catch (IOException ignore) {
        }

        activeClients.remove(client);

        client.close();

        log.info("Connections is closed :: " + clientAddress);
    }

    /**
     * Close all open connections and stop the this worker.
     */
    public void stop() {
        try {
            readWriteSelector.close();
        }
        catch (IOException e) {
            log.error("Selector can't be closed :: ", e);
        }

        for (SocketChannel registeredClient : activeClients) {
            try {
                registeredClient.close();
            }
            catch (IOException e) {
                log.error("Connection with client can't be closed :: ", e);
            }
        }
    }

    /**
     * Establish new connection with given client.
     *
     * @param socketChannel Channel with the remote client, which the connection should be established to.
     */
    public void establishNewConnection(SocketChannel socketChannel) {
        changesCallbacks.add(() -> {
            try {
                SelectionKey newClient = socketChannel.register(readWriteSelector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);

                newClient.attach(connectionHandlerFactory.apply(() -> readyForWrite(newClient)));

                activeClients.add(socketChannel);

                log.info("New connection established :: " + socketChannel.getRemoteAddress());
            }
            catch (IOException ex) {
                log.info("Registration of new connection failed :: ", ex);
            }
        });

        readWriteSelector.wakeup();
    }

    /**
     * Notify the worker that input selection key is ready to provide data for write.
     *
     * @param key Selection key which has a data for write.
     */
    public void readyForWrite(SelectionKey key) {
        changesCallbacks.add(() -> {
            if ((key.interestOps() & SelectionKey.OP_WRITE) == 0)
                key.interestOps(key.interestOps() | (SelectionKey.OP_WRITE));
        });

        readWriteSelector.wakeup();
    }
}
