package ar.edu.itba.pod.graphql.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseDao<T> {
    private final Map<String, T> data;
    private final Function<T, String> keyExtractor;


    protected BaseDao(final Function<T, String> keyExtractor) {
        this.data = new HashMap<>();
        this.keyExtractor = keyExtractor;
    }

    public void save(final T object) {
        data.put(keyExtractor.apply(object), object);
    }

    public void delete(final T object) {
        data.remove(keyExtractor.apply(object));
    }

    public Optional<T> findById(final String id) {
        return Optional.ofNullable(data.get((id)));
    }

    public ArrayList<T> getAll() {
        return new ArrayList<>(data.values());
    }
}
