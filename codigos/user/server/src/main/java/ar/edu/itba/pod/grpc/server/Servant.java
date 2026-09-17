package ar.edu.itba.pod.grpc.server;

import ar.edu.itba.pod.grpc.user.*;
import org.springframework.stereotype.Service;

import module java.base;

@Service
public class Servant extends UserServiceGrpc.UserServiceImplBase {

	/**
	 * Sample Data
	 */
	private static final Map<String, String> passwordsByUser = Map.of(
			"foo", "foopass",
			"bar", "barpass");
	private static final Map<String, User> users = Map.of(
			"foo", User.newBuilder()
					.setUserName("foo")
					.setDisplayName("Foo")
					.setStatus(AccountStatus.ACCOUNT_STATUS_ACTIVE)
					.addPreferences("darkMode")
					.addPreferences("liteView").build(),
			"bar", User.newBuilder()
					.setUserName("bar")
					.setDisplayName("Bar")
					.setStatus(AccountStatus.ACCOUNT_STATUS_PENDING)
					.addPreferences("lightMode").build());
	private final Map<String, UserRoles> userRolesMap = Map.of(
			"foo", UserRoles.newBuilder()
					.putRolesBySite("abc.com", Role.ADMIN)
					.putRolesBySite("xyz.com", Role.BUYER).build(),
			"bar", UserRoles.newBuilder()
					.putRolesBySite("abc.com", Role.SELLER).build());

}
