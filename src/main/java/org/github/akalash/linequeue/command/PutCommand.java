package org.github.akalash.linequeue.command;

import org.github.akalash.linequeue.storage.LineQueue;

/**
 * Storing new line to {@link LineQueue}.
 */
public class PutCommand implements Command {
    /** Lines storage. */
    private final LineQueue lineQueue;

    public PutCommand(LineQueue queue) {
        lineQueue = queue;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "PUT";
    }

    /** {@inheritDoc} */
    @Override public String execute(String newLine) {
        lineQueue.add(newLine);

        return NO_RESULT;
    }
}
