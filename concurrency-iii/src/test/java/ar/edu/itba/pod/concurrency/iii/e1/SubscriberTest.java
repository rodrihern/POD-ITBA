package ar.edu.itba.pod.concurrency.iii.e1;

import ar.edu.itba.pod.concurrency.iii.inmutable.Subscriber;
import ar.edu.itba.pod.concurrency.iii.inmutable.Subscription;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriberTest {

    @Test
    public final void test_immutability() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);

        String dateInString = "01-Jun-1983";

        Date dob = formatter.parse(dateInString);
        final List<Subscription> subscriptions = new LinkedList<>();
        subscriptions.add(new Subscription("magazine", Arrays.asList("livingF")));
        final Subscriber subscriber = new Subscriber(1, "john", dob, subscriptions);
        maliciousMethod(subscriber);

        assertAll("subscriber",
                () -> assertEquals(1, subscriber.getId()),
                () -> assertEquals("john", subscriber.getFullName()),
                () -> assertEquals(formatter.parse(dateInString), subscriber.getDateOfBirth()),
                () -> assertEquals(1, subscriber.getSubscriptions().size())
        );
    }

    private void maliciousMethod(final Subscriber subscriber) {
        subscriber.getDateOfBirth().setTime(10000L);
        subscriber.getSubscriptions().add(new Subscription("cable", Arrays.asList("sports")))
        ;
    }
}
