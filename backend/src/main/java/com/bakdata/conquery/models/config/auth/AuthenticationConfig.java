package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor
@Getter
public class AuthenticationConfig {

	// Define a maximum age since most browsers use session restoring making session cookies virtual permanent (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)
	@NotNull
	private Duration adminEndCookieDuration = Duration.hours(1);

	@JsonIgnore
	private AtomicReference<AuthCookieFilter> authCookieFilter = new AtomicReference<>();




	public NewCookie createAuthCookie(ContainerRequestContext request, String token) {
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

	public static NewCookie expireAuthCookie() {
		return new NewCookie(
				AuthCookieFilter.ACCESS_TOKEN,
				null,
				"/",
				null,
				null,
				0,
				false);
	}

	@JsonIgnore
	public AuthCookieFilter getAuthCookieFilter() {
		return authCookieFilter.updateAndGet(
				(f) -> f != null ? f : new AuthCookieFilter(this::createAuthCookie)
		);
	}
}
