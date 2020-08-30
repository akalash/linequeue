package org.github.akalash.linequeue.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A worker which await new connections on the open socket and proceed it for further handling.
 */
public class PortListenWorker implements Runnable {
    private static final Logger log = LogManager.getLogger(PortListenWorker.class);

    /** Local port for binding. */
    private final int localPort;

    /** Consumer which handle new input connection. */
    private final Consumer<SocketChannel> newConnectionHandler;

    /**
     *
     */
    private ServerSocketChannel serverChannel;

    public PortListenWorker(int port, Consumer<SocketChannel> register) {
        localPort = port;
        newConnectionHandler = register;
    }

    @Override public void run() {
        try {
            handleIncomingConnections();
        }
        catch (Exception ex) {
            log.error("Error during the handling incoming connections", ex);
        }
        finally {
            try {
                serverChannel.socket().close();
                serverChannel.close();
            }
            catch (IOException e) {
                log.error("Channel can't be closed :: ", e);
            }

            log.info("Handling of incoming connection was finished");
        }
    }

    /**
     * Endlessly waiting for new connections and sending to the handler.
     *
     * @throws IOException If fail.
     */
    private void handleIncomingConnections() throws IOException {
        Selector selector = Selector.open();

        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress hostAddress = new InetSocketAddress("localhost", localPort);
        serverChannel.bind(hostAddress);

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        log.info("Server bind to address :: " + hostAddress);

        while (!Thread.currentThread().isInterrupted()) {
            int keysCount = selector.select();

            if (keysCount == 0)
                continue;

            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                keyIterator.remove();

                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel)key.channel();

                    SocketChannel client = server.accept();
                    // Non Blocking I/O.
                    client.configureBlocking(false);

                    newConnectionHandler.accept(client);

                    log.info("New connection request was received from client :: " + client.getRemoteAddress());
                }
            }
        }

        selector.close();
    }
}
