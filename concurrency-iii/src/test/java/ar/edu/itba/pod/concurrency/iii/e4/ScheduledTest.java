package ar.edu.itba.pod.concurrency.iii.e4;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ScheduledTest {

    @Test
    public final void test() throws InterruptedException, ExecutionException {
        final ScheduledExecutorService executorService = Executors
                .newSingleThreadScheduledExecutor();
        try {

        } finally {

            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.SECONDS);
            executorService.shutdownNow();
        }
    }
}
