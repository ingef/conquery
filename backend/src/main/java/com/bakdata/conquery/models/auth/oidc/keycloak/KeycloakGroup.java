package com.bakdata.conquery.models.auth.oidc.keycloak;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakGroup(
	@NotEmpty String id,
	@NotEmpty String name,
	@NotEmpty String path,
	Map<String, String> attributes,
	Set<KeycloakGroup> subGroups
) {
}
