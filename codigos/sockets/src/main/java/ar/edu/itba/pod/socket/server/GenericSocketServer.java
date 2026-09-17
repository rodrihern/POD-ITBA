package ar.edu.itba.pod.socket.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class GenericSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(GenericSocketServer.class);

    private static final int PORT = 6666;
    private int visitCount = 0;

    public void start(int port) throws IOException {
        logger.info("starting server on port {}", port);
        try (ServerSocket server = new ServerSocket(port);
             Socket client = server.accept();
             var out = new PrintWriter(client.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(client.getInputStream()))
        ) {
            boolean loop = true;
            String inputLine;
            while (loop && (inputLine = in.readLine()) != null) {
                loop = handleClient(inputLine, out);
            }
        }
    }

    private boolean handleClient(String inputLine, PrintWriter out) {
        logger.debug("received message {}", inputLine);
        if ("1".equals(inputLine)) {
            visitCount++;
        }
        out.println(visitCount);
        return !".".equals(inputLine);
    }

    static void main() throws IOException {
        new GenericSocketServer().start(PORT);
    }

}
