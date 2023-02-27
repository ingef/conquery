package com.bakdata.conquery.models.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakApi;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakGroup;
import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Slf4j
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_SERVER_URL", matches = "^https?://.+", disabledReason = "As long as there is no mock server, test manually against a real server")
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_REALM", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_USER_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "KEYCLOAK_GROUP_ID", matches = ".+")
public class KeycloakApiTest {

	private static KeycloakApi keycloakApi;

	@BeforeAll
	public static void init() {
		IntrospectionDelegatingRealmFactory config = new IntrospectionDelegatingRealmFactory();

		config.setAuthServerUrl(System.getenv("KEYCLOAK_SERVER_URL"));
		config.setRealm(System.getenv("KEYCLOAK_REALM"));
		config.setResource(System.getenv("KEYCLOAK_CLIENT_ID"));
		config.setCredentials(Map.of("secret", System.getenv("KEYCLOAK_CLIENT_SECRET")));

		keycloakApi = new KeycloakApi(config, ClientBuilder.newClient());
	}

	@Test
	public void getUserGroups() {
		final String userId = System.getenv("KEYCLOAK_USER_ID");
		final Set<KeycloakGroup> userGroups = keycloakApi.getUserGroups(userId);
		log.info("User[{}] is in Groups: {}", userId, userGroups);

		assertThat(userGroups).isNotNull();
	}


	@Test
	public void getGroupHierarchy() {
		final Set<KeycloakGroup> groups = keycloakApi.getGroupHierarchy();
		log.info("Group Hierarchy: {}", groups);

		assertThat(groups).isNotNull();
	}

	@Test
	public void getGroup() {
		final String groupId = System.getenv("KEYCLOAK_GROUP_ID");
		final KeycloakGroup group = keycloakApi.getGroup(groupId);
		log.info("Group[{}]: {}", groupId, group);

		assertThat(group).isNotNull();
	}
}
