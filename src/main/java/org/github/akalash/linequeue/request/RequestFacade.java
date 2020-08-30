package org.github.akalash.linequeue.request;

import java.nio.ByteBuffer;

/**
 * It is an interface of interaction with commands -
 * it provide request to the handle and received corresponded response.
 */
public interface RequestFacade {
    /**
     * Notifying that the response was received from the command.
     *
     * @param in Received data.
     */
    void responseReceived(byte[] in);

    /**
     * Take the request which should be handled by command.
     *
     * @return Request for handle.
     */
    String nextRequest();

    /**
     * @return {@code true} if the current handler has more requests that await to be handled by command.
     */
    boolean hasNextRequest();

    /**
     * Finish all activity connected with this facade.
     */
    void finish();
}
