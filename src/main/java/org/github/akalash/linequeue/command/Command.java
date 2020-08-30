package org.github.akalash.linequeue.command;

/**
 * Common interface of all commands.
 */
public interface Command {
    static final String NO_RESULT = "";

    /**
     * Name of the command which should be the same as in request.
     *
     * @return Name of the command.
     */
    String name();

    /**
     * Execute specific logic of this command and return the response.
     *
     * @param payload Command specific input data from request.
     * @return Response which should be send to the client or {@code null} if response can be missed.
     */
    String execute(String payload);
}
