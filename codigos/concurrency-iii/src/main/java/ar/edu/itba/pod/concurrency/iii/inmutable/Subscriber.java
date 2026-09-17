package ar.edu.itba.pod.concurrency.iii.inmutable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Subscriber {
    private final Integer id;
    private final String fullName;
    private final Date dateOfBirth;
    private final List<Subscription> subscriptions;

    public Subscriber(Integer id, String fullName, Date dateOfBirth,  List<Subscription> subscriptions) {
        this.id = id;
        this.fullName = fullName;
        this.dateOfBirth = new Date(dateOfBirth.getTime());
        this.subscriptions = List.copyOf(subscriptions);
    }

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Date getDateOfBirth() {
        return new Date(dateOfBirth.getTime());
    }

    public List<Subscription> getSubscriptions() {
        return new ArrayList<>(subscriptions);
    }
}
