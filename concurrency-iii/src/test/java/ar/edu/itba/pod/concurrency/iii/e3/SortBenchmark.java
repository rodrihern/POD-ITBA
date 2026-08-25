package ar.edu.itba.pod.concurrency.iii.e3;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmar to compare between {@link Arrays#parallelSort(int[])} and
 * {@link Arrays#sort(int[])}
 */
public class SortBenchmark {

    @RepeatedTest(4)
    public void benchmark_all() {
        final Consumer<int[]> serial = a -> Arrays.sort(a);
        final Consumer<int[]> parallel = a -> Arrays.parallelSort(a);
        final int multiplier = 100_000;
        final Random random = new Random();

        final int[] small = generateArray(10 * multiplier, random);
        final int[] med = generateArray(25 * multiplier, random);
        final int[] large = generateArray(50 * multiplier, random);

        benchmark(small, serial, "Small serial");
        benchmark(small, parallel, "Small parallel");
        benchmark(med, serial, "med serial");
        benchmark(med, parallel, "med parallel");
        benchmark(large, serial, "large serial");
        benchmark(large, parallel, "large parallel");
    }

    private int[] generateArray(final int size, final Random random) {
        final int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }

        return array;
    }

    private void benchmark(final int[] array, final Consumer<int[]> orderer, final String message) {
        long accumulate = 0;
        for (int i = 0; i < 4; i++) {
            final long startTime = System.currentTimeMillis();
            orderer.accept(Arrays.copyOf(array, array.length));
            final long finishTime = System.currentTimeMillis();
            accumulate += finishTime - startTime;
        }

        System.out.println(message + " took " + (accumulate / 4) + " ms");
    }

}
