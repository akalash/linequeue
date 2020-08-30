package org.github.akalash.linequeue.request;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import org.github.akalash.linequeue.network.ConnectionFacade;

/**
 * Mediator between socket data and command data.
 */
public class DataExchanger implements ConnectionFacade, RequestFacade {
    private final ByteBuffer writeSocketBuffer = ByteBuffer.allocate(1024);

    /** Helper class for splitting monotonic input data to the string line. */
    private final LineTokenizer lineTokenizer = new LineTokenizer();

    /** Consumer which should be notified when new request is ready to handle. */
    private final Consumer<RequestFacade> requestReadyNotifier;

    /** Callback for notification the consumer that the data is ready to be written to the socket. */
    private final Runnable responseReadyNotifier;

    /** Prepared requests which can be handled. */
    private final Queue<String> requests = new ConcurrentLinkedDeque<>();

    /** Prepared responses that can be written to the socket. */
    private final Queue<ByteBuffer> responses = new ConcurrentLinkedDeque<>();

    private volatile boolean finish = false;

    public DataExchanger(Consumer<RequestFacade> requestReadyNotifier, Runnable responseReadyNotifier) {
        this.requestReadyNotifier = requestReadyNotifier;
        this.responseReadyNotifier = responseReadyNotifier;

        writeSocketBuffer.compact();
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer nextResponse() {
        if(finish)
            return null;

        if (writeSocketBuffer.hasRemaining())
            writeSocketBuffer.compact();
        else
            writeSocketBuffer.clear();

        ByteBuffer src = responses.peek();

        while (writeSocketBuffer.hasRemaining() && src != null) {
            if (!src.hasRemaining()) {
                responses.poll();

                src = responses.peek();
            }

            if (src != null)
                writeSocketBuffer.put(src.get());
        }

        writeSocketBuffer.flip();

        return writeSocketBuffer;
    }

    /** {@inheritDoc} */
    @Override public void requestReceived(ByteBuffer buffer) {
        requests.addAll(lineTokenizer.extractCompletedLines(buffer));

        requestReadyNotifier.accept(this);
    }

    /** {@inheritDoc} */
    @Override public void responseReceived(byte[] in) {
        responses.add(ByteBuffer.wrap(in));

        responseReadyNotifier.run();
    }

    /** {@inheritDoc} */
    @Override public String nextRequest() {
        return requests.poll();
    }

    /** {@inheritDoc} */
    @Override public boolean hasNextRequest() {
        return !requests.isEmpty() && !finish;
    }

    /** {@inheritDoc} */
    @Override public void finish() {
        finish = true;
    }

    /** {@inheritDoc} */
    @Override public boolean hasNextResponse() {
        return !finish && (writeSocketBuffer.hasRemaining() || !responses.isEmpty());
    }
}
