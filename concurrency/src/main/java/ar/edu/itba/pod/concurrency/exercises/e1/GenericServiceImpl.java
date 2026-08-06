package ar.edu.itba.pod.concurrency.exercises.e1;

import java.util.ArrayDeque;
// import java.util.Optional;
import java.util.Queue;

/**
 * Basic implementation of {@link GenericService}.
 */
public  class GenericServiceImpl implements GenericService {

    private int visits;
    private Queue<String> queue;

    public GenericServiceImpl() {
        this.visits = 0;
        this.queue = new ArrayDeque<>();
    }

    @Override
    public String echo(String message) {
        return message;
    }

    @Override
    public String toUpper(String message) {

        if (message == null) {
            return null;
        }
        return message.toUpperCase();

        // mas objetoso
        // return Optional.ofNullable(message).map(String::toUppercase).orElse(null);
    }

    @Override
    public void addVisit() {
        visits++;
    }

    @Override
    public int getVisitCount() {
        return visits;
    }

    @Override
    public boolean isServiceQueueEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void addToServiceQueue(String name) {
        queue.add(name);
    }

    @Override
    public String getFirstInServiceQueue() {
        if (isServiceQueueEmpty()) {
            throw new IllegalStateException("No one in queue");
        }

        return queue.poll();
    }
}
