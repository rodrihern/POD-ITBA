package ar.edu.itba.pod.concurrency.exercises.e3;

import ar.edu.itba.pod.concurrency.exercises.e1.GenericService;
import ar.edu.itba.pod.concurrency.exercises.e1.GenericServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutorTest {
    private GenericService service;

    @BeforeEach
    public final void before() {
        service = new GenericServiceImpl();
    }

    @Test
    public final void test() {
        assertEquals("No one in queue", "No one in queue");
    }
}
