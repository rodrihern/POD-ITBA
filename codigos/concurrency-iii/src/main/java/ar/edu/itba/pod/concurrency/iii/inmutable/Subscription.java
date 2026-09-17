package ar.edu.itba.pod.concurrency.iii.inmutable;

import java.util.ArrayList;
import java.util.List;

public class Subscription {
    private final String name;
    private final List<String> tags;


    public Subscription(String name, List<String> tags) {
        this.name = name;
        this.tags = List.copyOf(tags);
    }

    public String getName() {
        return name;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
}
