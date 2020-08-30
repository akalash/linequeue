package org.github.akalash.linequeue.request;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.github.akalash.linequeue.command.Command;
import org.github.akalash.linequeue.command.CommandExecutor;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class LineTokenizerTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(new Object[][] {
            {asList("line\r\n"), asList("line\r\n")},
            {asList("line\r\n2"), asList("line\r\n")},
            {asList("line\r2"), asList("line\r")},
            {asList("line\n2"), asList("line\n")},
            {asList("line2"), Collections.emptyList()},
            {asList("multi\r\n", "line\r\n"), asList("multi\r\n", "line\r\n")},
            {asList("multi\r\nline\r\n in one\r\n input"), asList("multi\r\n", "line\r\n", " in one\r\n")},
            {asList("multi\r\n", "line\r\n3"), asList("multi\r\n", "line\r\n")},
            {asList("multi ", "line\r\n3"), asList("multi line\r\n")},
            {asList("one ", "more", " multi ", "line\r\n3"), asList("one more multi line\r\n")},
//            {asList("multi\r", "\nline\r\n"), asList("multi\r\n", "line\r\n")}, //TODO: fix this scenario.
        });
    }

    @Test
    public void extractCompletedLines() {
        LineTokenizer tokenizer = new LineTokenizer();

        List<String> result = new ArrayList<>();

        inputLine.forEach(
            line -> result.addAll(tokenizer.extractCompletedLines(ByteBuffer.wrap(line.getBytes())))
        );

        assertThat(result, Matchers.is(expectedResult));
    }

    private final List<String> inputLine;
    private final List<String> expectedResult;

    public LineTokenizerTest(List<String> line, List<String> expectedResult) {
        inputLine = line;
        this.expectedResult = expectedResult;
    }
}