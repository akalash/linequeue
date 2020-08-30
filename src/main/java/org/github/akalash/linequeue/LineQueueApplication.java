package org.github.akalash.linequeue;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.akalash.linequeue.command.CommandExecutor;
import org.github.akalash.linequeue.command.GetCommand;
import org.github.akalash.linequeue.command.PutCommand;
import org.github.akalash.linequeue.command.QuitCommand;
import org.github.akalash.linequeue.command.ShutdownCommand;
import org.github.akalash.linequeue.network.PortListenWorker;
import org.github.akalash.linequeue.network.ReadWriteSocketWorker;
import org.github.akalash.linequeue.request.DataExchanger;
import org.github.akalash.linequeue.request.RequestExecutionWorker;
import org.github.akalash.linequeue.request.RequestRegistry;
import org.github.akalash.linequeue.storage.LineQueue;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Entry point of application which configure and start all activity.
 */
public class LineQueueApplication {
    private static final Logger log = LogManager.getLogger(LineQueueApplication.class);

    private ExecutorService commandExecutionService;
    private ExecutorService readWriteSocketService;
    private ExecutorService newConnectionService;

    /** Start the application. */
    public void start(
        String dumpFilePath,
	int port,
        int commandExecutorThreadCount,
        int readWriteSocketThreadCount,
        int listenPortThreadCount	
    ) throws IOException {
        LineQueue lineQueue = new LineQueue(dumpFilePath);

        if (!lineQueue.restore())
            return;

        commandExecutionService = newFixedThreadPool(commandExecutorThreadCount, new ThreadNamedFactory("command-executor-"));
        readWriteSocketService = newFixedThreadPool(readWriteSocketThreadCount, new ThreadNamedFactory("read-write-socket-"));
        newConnectionService = newFixedThreadPool(listenPortThreadCount, new ThreadNamedFactory("port-listener-"));

        RequestRegistry requestRegistry = new RequestRegistry();

        CommandExecutor commandExecutor = new CommandExecutor(Arrays.asList(
            new PutCommand(lineQueue),
            new GetCommand(lineQueue),
            new ShutdownCommand(lineQueue, this::stop),
            new QuitCommand()
        ));

        RequestExecutionWorker requestExecutionWorker = new RequestExecutionWorker(requestRegistry, commandExecutor);

        ReadWriteSocketWorker readWriteSocketWorker = new ReadWriteSocketWorker(
            (responseReadyNotifier) -> new DataExchanger(requestRegistry::offer, responseReadyNotifier)
        );

        PortListenWorker portListenWorker =
            new PortListenWorker(port, readWriteSocketWorker::establishNewConnection);

        commandExecutionService.submit(requestExecutionWorker);
        readWriteSocketService.submit(readWriteSocketWorker);
        newConnectionService.submit(portListenWorker);
    }

    /** Stop all activity. */
    public void stop() {
        newConnectionService.shutdownNow();

        try {
            newConnectionService.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            log.error("Accept connection worker is failed during stop.", e);
        }

        readWriteSocketService.shutdownNow();
        try {
            readWriteSocketService.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            log.error("Read-write socket worker is failed during stop.", e);
        }

        commandExecutionService.shutdownNow();
        try {
            commandExecutionService.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            log.error("Command execution worker is failed during stop.", e);
        }
    }

    public static void main(String[] args) throws IOException {
        new LineQueueApplication().start(
            "line_queue.dump",
	    10042,
            1,
            2,
            4
        );
    }

    /**
     * Thread factory which allows to customize the thread name.
     */
    private static class ThreadNamedFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger number = new AtomicInteger();

        private ThreadNamedFactory(String prefix) {
            namePrefix = prefix;
        }

        /** {@inheritDoc} */
        @Override public Thread newThread(Runnable r) {
            return new Thread(r, namePrefix + number.getAndIncrement());
        }
    }
}
