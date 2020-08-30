package org.github.akalash.linequeue.request;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Helper class for splitting monotonic input data to the string line. It stateful class which temporarily stored the
 * data which cannot be used to build the line immediately.
 */
public class LineTokenizer {
    /** Data that cannot be used to build a line. It awaits more data for the next try. */
    private final Queue<byte[]> unfinishedLines = new LinkedList<>();

    /** Total size of unfinished data. */
    private int unfinishedSize = 0;

    /**
     * Extract completed a line from a given input and from earlier stored unfinished lines.
     *
     * @param input Data which line should be extracted from.
     * @return Completed lines.
     */
    public List<String> extractCompletedLines(ByteBuffer input) {
        int start = 0, size = input.remaining();
        boolean endOfLine = false;

        List<String> res = new ArrayList<>();

        byte[] temp = new byte[size];

        for (int i = 0; input.hasRemaining(); i++) {
            byte b = input.get();
            temp[i] = b;

            if (b == '\n' || b == '\r')
                endOfLine = true;
            else if (endOfLine) {
                makeLine(res, temp, start, i);

                start = i;
                endOfLine = false;
            }
        }
        if (endOfLine)
            makeLine(res, temp, start, size);
        else {
            unfinishedLines.add(Arrays.copyOfRange(temp, start, size));
            unfinishedSize += size - start;
        }

        return res;
    }

    /**
     * Make new line from 'start' to 'end' using 'temp' array and unfinished lines.
     *
     * @param res List which result should be stored to.
     * @param temp Array which will be used to make line.
     * @param start Start position in temp.
     * @param end End position in temp.
     */
    private void makeLine(List<String> res, byte[] temp, int start, int end) {
        if (start != end) {
            byte[] line = new byte[unfinishedSize + (end - start)];
            int ind = 0;
            for (byte[] unfinishedLine : unfinishedLines) {
                for (byte b1 : unfinishedLine)
                    line[ind++] = b1;
            }

            for (int j = start; j < end; j++)
                line[ind++] = temp[j];

            unfinishedSize = 0;
            unfinishedLines.clear();
            res.add(new String(line));
        }
    }
}
