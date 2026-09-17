package ar.edu.itba.pod.concurrency.iii.e2;

import ar.edu.itba.pod.concurrency.iii.pubsub.NumbersConsumer;
import ar.edu.itba.pod.concurrency.iii.pubsub.NumbersProducer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PubSubTest {

    private final int BOUND = 10;
    private final int N_PRODUCERS = 4;
    private final int N_CONSUMERS = Runtime.getRuntime().availableProcessors();
    private final int poisonPill = Integer.MAX_VALUE;
    private final int poisonPillPerProducer = N_CONSUMERS / N_PRODUCERS;
    private final int mod = N_CONSUMERS % N_PRODUCERS;


    @Test
    public final void test_blocking_queue_test() throws InterruptedException {
        final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(BOUND);

        final ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            for (int i = 1; i < N_PRODUCERS; i++) {
                executorService.submit(new NumbersProducer(queue, poisonPill, poisonPillPerProducer));
            }

            for (int j = 0; j < N_CONSUMERS; j++) {
                executorService.submit(new NumbersConsumer(queue, poisonPill));
            }

            executorService.submit(new NumbersProducer(queue, poisonPill, poisonPillPerProducer + mod));
        } finally {

            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.SECONDS);
        }
        assertEquals(0, queue.size());
    }
}
