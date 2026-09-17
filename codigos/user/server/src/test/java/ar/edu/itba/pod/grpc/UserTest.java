package ar.edu.itba.pod.grpc;

import ar.edu.itba.pod.grpc.server.HealthConfig;
import ar.edu.itba.pod.grpc.server.Servant;
import ar.edu.itba.pod.grpc.user.UserServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBootTest
@SpringJUnitConfig(UserTest.TestConfig.class)
@AutoConfigureInProcessTransport
public class UserTest {
	@Autowired
	private UserServiceGrpc.UserServiceBlockingStub stub;

	@Test
	void loginTest() {
		// TODO
	}

	@EnableAutoConfiguration
	@Import({ Servant.class, HealthConfig.class })
	@ImportGrpcClients(types = UserServiceGrpc.UserServiceBlockingStub.class)
	static class TestConfig {
	}

}