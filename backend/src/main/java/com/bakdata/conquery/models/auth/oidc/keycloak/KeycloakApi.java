package com.bakdata.conquery.models.auth.oidc.keycloak;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import com.bakdata.conquery.util.ResourceUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeycloakApi {

	private static final String USER_ID_TEMPLATE = "user_id";
	private static final String GROUP_ID_TEMPLATE = "group_id";

	private final WebTarget user;
	private final WebTarget userGroups;
	private final WebTarget groups;
	private final WebTarget group;

	public KeycloakApi(IntrospectionDelegatingRealmFactory config, Client client) {

		client.register(new ClientCredentialsGrantRequestFilter(config.getClientId(), config.getClientSecret(), URI.create(config.getTokenEndpoint())));

		final WebTarget base = client.target(config.getAuthServerUrl());

		final WebTarget adminBase = base.path("admin").path("realms").path(config.getRealm());

		user = adminBase.path("users").path(ResourceUtil.wrapAsUriTemplate(USER_ID_TEMPLATE));
		userGroups = user.path("groups");

		groups = adminBase.path("groups");
		group = groups.path(ResourceUtil.wrapAsUriTemplate(GROUP_ID_TEMPLATE));
	}

	public Set<KeycloakGroup> getUserGroups(String userId) {
		Preconditions.checkNotNull(userId);
		return userGroups.resolveTemplate(USER_ID_TEMPLATE, userId).request().get(new GenericType<Set<KeycloakGroup>>() {
		});
	}


	public Set<KeycloakGroup> getGroupHierarchy() {
		final WebTarget webTarget = groups;
		log.info("Requesting group from: {}", webTarget.getUri());
		return webTarget.request().get(new GenericType<Set<KeycloakGroup>>() {
		});
	}


	public KeycloakGroup getGroup(String groupId) {
		Preconditions.checkNotNull(groupId);
		final WebTarget webTarget = group.resolveTemplate(GROUP_ID_TEMPLATE, groupId);
		log.info("Requesting group from: {}", webTarget.getUri());
		return webTarget.request().get(KeycloakGroup.class);
	}
}
