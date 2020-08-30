package org.github.akalash.linequeue.request;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe holder of all requests which awaits of handling.
 */
public class RequestRegistry {
    /**
     * These are facades which contains request which should be handled. Facade used here instead of request in order of
     * prevention of reordering from one client.
     */
    private final LinkedBlockingQueue<RequestFacade> requestFacades = new LinkedBlockingQueue<>();

    /** Facades that were added to a queue to handling already. */
    private final Set<RequestFacade> awaitedFacades = new ConcurrentSkipListSet<>(Comparator.comparing(Object::toString));

    /** Add a new facade to handling. */
    public void offer(RequestFacade requestFacade) {
        if (awaitedFacades.add(requestFacade))
            requestFacades.add(requestFacade);
    }

    /** Get first facade for handling. */
    public RequestFacade poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return requestFacades.poll(timeout, timeUnit);
    }

    /** Mark that the facade was handled. */
    public void markAsDone(RequestFacade requestFacade) {
        awaitedFacades.remove(requestFacade);
    }
}
