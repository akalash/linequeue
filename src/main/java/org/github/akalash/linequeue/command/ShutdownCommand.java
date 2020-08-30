package org.github.akalash.linequeue.command;

import org.github.akalash.linequeue.storage.LineQueue;

/**
 *  Finishing all work on current instance, dumping all data from memory to disk and quit the process.
 */
public class ShutdownCommand implements Command {
    /** Lines storage. */
    final LineQueue lineQueue;

    /** The delegate which should stop all instance activity. */
    private final Runnable stopAction;

    public ShutdownCommand(LineQueue queue, Runnable action) {
        lineQueue = queue;
        stopAction = action;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "SHUTDOWN";
    }

    /** {@inheritDoc} */
    @Override public String execute(String payload) {
        new Thread(() -> {
            stopAction.run();

            lineQueue.dump();
        }).start();

        return NO_RESULT;
    }
}
