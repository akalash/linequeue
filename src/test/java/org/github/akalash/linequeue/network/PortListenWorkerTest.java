package org.github.akalash.linequeue.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PortListenWorkerTest {

    @Test
    public void shouldSuccessfullyConnectedToWorker() throws IOException, InterruptedException {
        AtomicReference<SocketAddress> connectionEstablished = new AtomicReference();
        CountDownLatch awaitConnection = new CountDownLatch(1);

        int testPort = 10049;

        new Thread(new PortListenWorker(testPort, socketChannel -> {
            try {
                connectionEstablished.set(socketChannel.getRemoteAddress());
            }
            catch (IOException ignore) {
            }
            awaitConnection.countDown();
        })).start();

        //TODO: detect bind to socket differently.
        Thread.sleep(1_000);

        SocketChannel testClient = SocketChannel.open(new InetSocketAddress("localhost", testPort));

        awaitConnection.await(5, TimeUnit.SECONDS);

        assertThat(connectionEstablished.get(), is(testClient.getLocalAddress()));
    }
}