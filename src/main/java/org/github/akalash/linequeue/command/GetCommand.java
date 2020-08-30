package org.github.akalash.linequeue.command;

import java.util.List;
import org.github.akalash.linequeue.storage.LineQueue;

/**
 * Returning of the number of requested lines from {@link LineQueue}.
 */
public class GetCommand implements Command {
    /** Lines storage. */
    private final LineQueue lineQueue;

    public GetCommand(LineQueue lineQueue) {
        this.lineQueue = lineQueue;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "GET";
    }

    /** {@inheritDoc} */
    @Override public String execute(String lineCount) {
        List<String> poll = lineQueue.poll(Integer.parseInt(lineCount.trim()));

        if (poll == null)
            throw new IllegalArgumentException("Number of requested lines are incorrect");

        return String.join("", poll);
    }
}
