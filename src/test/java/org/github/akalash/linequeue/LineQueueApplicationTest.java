package org.github.akalash.linequeue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class LineQueueApplicationTest {
    private static final int TEST_PORT = 10052;
    private static final String DUMP_FILE = "test.dump";

    @Test
    public void oneClient() throws IOException, InterruptedException, ExecutionException {
        LineQueueApplication app = new LineQueueApplication();
        new File(DUMP_FILE).delete();

        app.start(DUMP_FILE, TEST_PORT, 4, 2, 1);

        //TODO: detect bind to socket differently.
        Thread.sleep(2000);

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<List<String>> future = executorService.submit(makeClient(Arrays.asList(
            "PUT 1\r\n",
            "PUT 2\r\n",
            "PUT 3\r\n",
            "PUT 4\r\n",
            "GET 2\r\n",
            "PUT 5\r\n",
            "GET 1\r\n",
            "PUT 6\r\n",
            "SHUTDOWN\r\n"
        )));

        List<String> responses = future.get();

        assertThat(responses, contains("1\r\n2\r\n", "3\r\n"));
        assertThat(responses, hasSize(2));

        //TODO: detect stop the server differently.
        Thread.sleep(2000);

        app.start("test.dump", TEST_PORT, 4, 2, 1);

        //TODO: detect bind to socket differently.
        Thread.sleep(2000);

        future = executorService.submit(makeClient(Arrays.asList(
            "GET 2\r\n",
            "GET 2\r\n",
            "GET 1\r\n",
            "PUT 7\r\n",
            "QUIT\r\n"
        )));

        responses = future.get();

        assertThat(responses, contains("4\r\n5\r\n", "ERR\r\n", "6\r\n"));
        assertThat(responses, hasSize(3));

        future = executorService.submit(makeClient(Arrays.asList(
            "GET 1\r\n",
            "SHUTDOWN\r\n"
        )));

        responses = future.get();

        assertThat(responses, contains("7\r\n"));
        assertThat(responses, hasSize(1));

        app.stop();
    }

    @Test
    public void severalClients() throws IOException, InterruptedException, ExecutionException {
        LineQueueApplication app = new LineQueueApplication();

        try {
            app.start(DUMP_FILE, TEST_PORT, 4, 2, 1);

            //TODO:
            Thread.sleep(2000);

            int clientCount = 5;

            ExecutorService executorService = Executors.newFixedThreadPool(clientCount);

            List<Future<List<String>>> clientFut = new ArrayList<>();
            for (int i = 0; i < clientCount; i++) {
                clientFut.add(executorService.submit(makeClient(Arrays.asList(
                    "PUT 1\r\n",
                    "PUT 2\r\n",
                    "PUT 3\r\n",
                    "PUT 4\r\n",
                    "GET 2\r\n",
                    "PUT 5\r\n",
                    "GET 1\r\n",
                    "PUT 6\r\n"
                ))));
            }

            for (Future<List<String>> fut : clientFut) {
                assertThat(fut.get(), not(containsInAnyOrder("ERR\r\n")));
            }
        }
        finally {
            app.stop();
        }
    }

    private Callable<List<String>> makeClient(List<String> commands) {
        return () -> {
            List<String> result = new ArrayList<>();
            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress("localhost", TEST_PORT));

                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                ThreadLocalRandom random = ThreadLocalRandom.current();

                for (String command : commands) {
                    Thread.sleep(10 + random.nextLong(200));

                    ByteBuffer buffer = ByteBuffer.wrap(command.getBytes());

                    client.write(buffer);

                    buffer.clear();

                    System.out.println("Send :: " + command);

                    if (command.startsWith("GET")) {
                        client.read(readBuffer);

                        readBuffer.flip();

                        String response = StandardCharsets.UTF_8.decode(readBuffer).toString();

                        result.add(response);

                        System.out.println("Received :: " + response);

                        readBuffer.clear();
                    }
                }

                client.close();
            }
            catch (IOException | InterruptedException ex) {

            }

            return result;
        };
    }
}