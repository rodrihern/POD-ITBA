package ar.edu.itba.pod.common;

public record RentalsBetweenStationsResult(String startStation, String endStation, long rentals) {

    @Override
    public String toString() {
        return "%s -> %s with %d trips".formatted(startStation, endStation, rentals);
    }

}
