package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

public class AuthenticationConfig {

	// Define a maximum age since most browsers use session restoring making session cookies virtual permanent (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)
	private Duration adminEndCookieDuration = Duration.hours(1);

	@JsonIgnore
	private AuthCookieFilter authCookieFilterInstance = null;




	public Cookie createAuthCookie(ContainerRequestContext request, String token) {
		return new NewCookie(
				AuthCookieFilter.ACCESS_TOKEN,
				token,
				"/",
				null,
				0,
				null,
				Long.valueOf(adminEndCookieDuration.toSeconds()).intValue(),
				null,
				request.getSecurityContext().isSecure(),
				true
		);
	}

	@JsonIgnore
	public AuthCookieFilter getAuthCookieFilter() {
		if (authCookieFilterInstance == null) {
			synchronized (this) {
				if (authCookieFilterInstance == null) {
					authCookieFilterInstance = new AuthCookieFilter(this::createAuthCookie);
				}
			}
		}
		return authCookieFilterInstance;
	}
}
