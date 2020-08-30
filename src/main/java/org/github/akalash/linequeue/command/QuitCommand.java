package org.github.akalash.linequeue.command;

/**
 * Finishing all work on current instance and quit the process.
 */
public class QuitCommand implements Command {
    public QuitCommand() {

    }

    /** {@inheritDoc} */
    @Override public String name() {
        return "QUIT";
    }

    /** {@inheritDoc} */
    @Override public String execute(String notExpected) {
        return null;//For now, Null is sign of the close the client. TODO: Migrate from string to some ResultObject.
    }
}
