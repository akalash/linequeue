package org.github.akalash.linequeue.network;

import java.nio.ByteBuffer;

/**
 * It is an interface of interaction with socket -
 * it received data from the socket and return data in order to write to the socket.
 */
public interface ConnectionFacade {
    /**
     * Notifying that the data was received from the connection.
     *
     * @param buffer Received data.
     */
    public void requestReceived(ByteBuffer buffer);

    /**
     * Take the data which should be written to the socket.
     *
     * @return Data for write.
     */
    public ByteBuffer nextResponse();

    /**
     * @return {@code true} if the current handler has more data that await to be written to the socket.
     */
    public boolean hasNextResponse();
}
