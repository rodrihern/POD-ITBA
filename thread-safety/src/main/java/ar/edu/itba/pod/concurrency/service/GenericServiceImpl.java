package ar.edu.itba.pod.concurrency.service;

import ar.edu.itba.pod.concurrency.service.GenericService;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * Basic implementation of {@link GenericService}.
 */
public class GenericServiceImpl implements GenericService {
    private int visitCount;

    private final Queue<String> queue;

    public GenericServiceImpl() {
        queue = new LinkedList<>();
        visitCount = 0;
    }

    @Override
    public String echo(String message) {
        return message;
    }

    @Override
    public String toUpper(String message) {
        Optional<String> optional = Optional.ofNullable(message);
        Optional<String> s = optional.map(m -> m.toUpperCase());

        return s.orElse(null);
    }

    @Override
    public synchronized void addVisit() {
        visitCount++;
    }

    @Override
    public synchronized int getVisitCount() {
        return visitCount;
    }

    @Override
    public boolean isServiceQueueEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void addToServiceQueue(String name) {
        if (name == null) {
            throw new NullPointerException("null");
        }
        queue.add(name);
    }

    @Override
    public String getFirstInServiceQueue() {
        Optional<String> name = Optional.ofNullable(queue.poll());
        return name.orElseThrow(() -> new IllegalStateException("No one in queue"));
    }
}