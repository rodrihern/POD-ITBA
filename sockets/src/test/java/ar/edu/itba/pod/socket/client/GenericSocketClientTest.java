package ar.edu.itba.pod.socket.client;

import ar.edu.itba.pod.socket.server.GenericSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class GenericSocketClientTest {

    private static final Logger logger = LoggerFactory.getLogger(GenericSocketClientTest.class);

    private static final int PORT = 6666;
    private static final String HOST = "127.0.0.1";
    private GenericSocketClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new GenericSocketClient();
        client.startConnection(HOST, PORT);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.stopConnection();
    }

    @Test
    @Disabled
    public final void sendMessageTest() throws IOException {
        logger.info("testing server");
        assertEquals("1", client.sendMessage("1"));
        assertEquals("2", client.sendMessage("1"));
        assertEquals("2", client.sendMessage("2"));
        assertEquals("2", client.sendMessage("."));
    }

}