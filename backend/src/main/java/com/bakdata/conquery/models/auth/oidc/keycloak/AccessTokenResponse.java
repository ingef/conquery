package com.bakdata.conquery.models.auth.oidc.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object from oauth token endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessTokenResponse(
		@JsonProperty("access_token") String access_token,
		@JsonProperty("expires_in") long expires_in,
		@JsonProperty("token_type") String token_type
) {
}
