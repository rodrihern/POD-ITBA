package ar.edu.itba.pod.grpc;

import ar.edu.itba.pod.grpc.server.Servant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GreeterTest.TestConfig.class)
@AutoConfigureInProcessTransport
public class GreeterTest {

	// TODO

	@Test
	void sayHelloTest() {
		// TODO
	}

	@EnableAutoConfiguration
	@Import({ Servant.class })
	// TODO
	static class TestConfig {
	}

}