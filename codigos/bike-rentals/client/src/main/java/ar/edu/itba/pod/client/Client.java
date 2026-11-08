package ar.edu.itba.pod.client;

import ar.edu.itba.pod.common.BikeRentalRow;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.KeyValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    static void main(String[] args) throws IOException {
        logger.info("hz-bike-rentals Client Starting ...");

        try {
            // Group Config
            GroupConfig groupConfig = new GroupConfig().setName("l65522").setPassword("l65522-pass");

            // Client Network Config
            ClientNetworkConfig clientNetworkConfig = new ClientNetworkConfig();
            clientNetworkConfig.addAddress("127.0.0.1");

            // Client Config
            ClientConfig clientConfig = new ClientConfig().setGroupConfig(groupConfig).setNetworkConfig(clientNetworkConfig);

            // Node Client
            HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

            // Key Value Source
            MultiMap<String, BikeRentalRow> rentalsMultiMap = hazelcastInstance.getMultiMap("rentalsByStartStation");
            KeyValueSource<String, BikeRentalRow> rentalsKeyValueSource = KeyValueSource.fromMultiMap(rentalsMultiMap);

            // Job Tracker
            JobTracker jobTracker = hazelcastInstance.getJobTracker("bikeRentals");

            // Text File Reading and Key Value Source Loading
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try (Stream<String> lines = Files.lines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
                lines.skip(1)
                        .map(line -> line.split(";"))
                        .map(line -> new BikeRentalRow(
                                line[0],
                                LocalDateTime.parse(line[1], dateTimeFormatter),
                                line[2],
                                LocalDateTime.parse(line[3], dateTimeFormatter),
                                Integer.parseInt(line[4]) == 1))
                        .forEach(bikeRental -> rentalsMultiMap.put(bikeRental.getStartStation(), bikeRental));
            }

            // Ejercicio 2
            // Check how many objects where loaded
            IO.println(rentalsMultiMap.size());

            // Ejercicio 3
            // TODO
        } finally {
            HazelcastClient.shutdownAll();
        }
    }

}
