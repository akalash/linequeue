package org.github.akalash.linequeue.command;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.github.akalash.linequeue.command.CommandExecutor.ERROR;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class CommandExecutorTest {

    public static final String TEST_COMMAND_NAME = "TEST";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"TEST 123", "123"},
            {"TEST 1 w D 9 ", "1 w D 9 "},
            {" TEST 123", ERROR},
            {"TEST123", ERROR},
            {"FF", ERROR},
            {"TEST", null},
            {"TEST  ", " "},
        });
    }

    @Test
    public void shouldCorrectlyChooseCommandToExecute() {
        CommandExecutor executor = new CommandExecutor(Arrays.asList(new Command() {

            @Override public String name() {
                return TEST_COMMAND_NAME;
            }

            @Override public String execute(String payload) {
                return payload;
            }
        }));

        String res = executor.handleRequest(request);

        assertThat(res, is(expectedResult));
    }

    private final String request;
    private final String expectedResult;

    public CommandExecutorTest(String request, String expectedResult) {
        this.request = request;
        this.expectedResult = expectedResult;
    }
}