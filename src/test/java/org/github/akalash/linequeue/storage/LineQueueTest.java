package org.github.akalash.linequeue.storage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LineQueueTest {

    @Test
    public void oneThreadAddPollScenario() {
        LineQueue queue = new LineQueue("");

        //Adding some values.
        queue.add("a");
        queue.add("b");
        queue.add("c");

        //Assert that poll for 1 works well.
        assertThat(queue.poll(1), contains("a"));

        //Adding after poll.
        queue.add("d");

        //Assert that poll for more than 1 works well.
        assertThat(queue.poll(3), contains("b", "c", "d"));

        //Assert that poll for empty queue works well.
        assertThat(queue.poll(1), nullValue());

        //Adding after empty queue.
        queue.add("e");

        assertThat(queue.poll(1), contains("e"));
    }

    @Test
    public void dumpRestoreScenario() {
        String dumpFilePath = "test.dump";

        LineQueue queue = new LineQueue(dumpFilePath);

        //Adding some values.
        queue.add("a");
        queue.add("b");
        queue.add("c");

        queue.dump();

        //Create another queue for checking restore.
        LineQueue restoredQueue = new LineQueue(dumpFilePath);

        assertTrue(restoredQueue.restore());

        assertThat(queue.poll(3), contains("a", "b", "c"));
        assertThat(queue.poll(1), nullValue());
        assertFalse(Files.exists(Paths.get(dumpFilePath)));
    }

    @Test
    public void multiThreadAddPollScenario() throws ExecutionException, InterruptedException {
        LineQueue queue = new LineQueue("");

        int totalEntryPerThread = 1_000_000;
        int totalPutThreads = 3;
        int totalReadThreads = 2;

        ExecutorService executorService = Executors.newFixedThreadPool(totalPutThreads + totalReadThreads);

        List<Future> jobs = new ArrayList<>();

        //Start 3 threads to put data to queue.
        for (int i = 0; i < totalPutThreads; i++) {
            int index = i;
            jobs.add(executorService.submit(() -> {

                int i1 = totalEntryPerThread;
                while (i1-- > 0)
                    queue.add(index + ":" + i1);
            }));
        }

        ConcurrentSkipListSet<String> result = new ConcurrentSkipListSet<>();
        AtomicBoolean stop = new AtomicBoolean();

        //Start 2 threads to read data from queue.
        for (int i = 0; i < totalReadThreads; i++) {
            int index = i + 1;
            executorService.submit(() -> {
                while (!stop.get()) {
                    List<String> list = queue.poll(index);
                    if (list != null)
                        result.addAll(list);
                }
            });
        }

        //Await when put threads finish their work.
        for (Future fut : jobs) {
            fut.get();
        }

        stop.set(true);

        //Read rest data.
        List<String> res;
        do {
            res = queue.poll(1);

            if (res != null)
                result.addAll(res);
        }
        while (res != null);

        //Ensure that number of unique data the same as expected.
        assertThat(result.size(), is(totalEntryPerThread * totalPutThreads));
    }
}