package org.github.akalash.linequeue.request;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.akalash.linequeue.command.CommandExecutor;

/**
 * A worker which take the next available request and executes it in corresponded command. It is guaranteed that
 * requests from one client will be handled in the same order as they were received.
 */
public class RequestExecutionWorker implements Runnable {
    private static final Logger log = LogManager.getLogger(RequestExecutionWorker.class);

    /** Holder of request which wait for the handling. */
    private final RequestRegistry requestRegistry;

    /** The executor of command which chooses the command according to the request. */
    private final CommandExecutor commandExecutor;

    public RequestExecutionWorker(RequestRegistry requestRegistry, CommandExecutor executor) {
        this.requestRegistry = requestRegistry;
        commandExecutor = executor;
    }

    @Override public void run() {
        try {
            handleRequests();
        }
        catch (Exception ex) {
            if (!(ex instanceof InterruptedException))
                log.error("Error during execution of request", ex);
        }
        finally {
            log.info("Execution of request was finished");
        }
    }

    /**
     * Awaiting of request which should be handled by command and write result as response.
     *
     * @throws InterruptedException When current thread was finished.
     */
    private void handleRequests() throws InterruptedException {
        RequestFacade requestFacade = null;

        while (!Thread.currentThread().isInterrupted() || requestFacade != null) {
            requestFacade = requestRegistry.poll(1000, TimeUnit.MILLISECONDS);

            if (requestFacade != null && requestFacade.hasNextRequest()) {
                String request = requestFacade.nextRequest();

                String result = commandExecutor.handleRequest(request);

                if (result == null)
                    requestFacade.finish();
                else if (!result.isEmpty())
                    requestFacade.responseReceived(result.getBytes());

                requestRegistry.markAsDone(requestFacade);

                if (requestFacade.hasNextRequest())
                    requestRegistry.offer(requestFacade);
            }
        }
    }
}
