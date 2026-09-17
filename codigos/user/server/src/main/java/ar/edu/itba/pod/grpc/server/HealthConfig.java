package ar.edu.itba.pod.grpc.server;

import ar.edu.itba.pod.grpc.user.UserServiceGrpc;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

	@Bean
	HealthStatusManager healthStatusManager() {
		var healthStatusManager = new HealthStatusManager();
		healthStatusManager.setStatus(UserServiceGrpc.SERVICE_NAME, HealthCheckResponse.ServingStatus.SERVING);
		return healthStatusManager;
	}

}
