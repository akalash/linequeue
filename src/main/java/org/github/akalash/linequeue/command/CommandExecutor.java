package org.github.akalash.linequeue.command;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Choose and execute the one of available command.
 */
public class CommandExecutor {
    /** Error message. */
    static final String ERROR = "ERR\r\n";

    /** Holder of all available command. */
    private final Map<String, Command> commandMap;

    public CommandExecutor(List<Command> availableCommands) {
        commandMap = availableCommands.stream().collect(Collectors.toMap(Command::name, identity()));
    }

    /**
     * Execute command and return its result according to the request.
     *
     * @param request Request which should be handled.
     * @return Result for the response to the client.
     */
    public String handleRequest(String request) {
        int index = request.indexOf(' ');

        String commandName = index == -1 ? request.trim() : request.substring(0, index);
        String payload = index == -1 ? null : request.substring(commandName.length() + 1);

        Command command = commandMap.get(commandName);

        if (command == null)
            return ERROR;

        try {
            return command.execute(payload);
        }
        catch (IllegalArgumentException ex) {
            return ERROR;
        }
    }
}
