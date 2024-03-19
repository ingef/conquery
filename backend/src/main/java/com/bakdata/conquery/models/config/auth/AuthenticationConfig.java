package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.NewCookie;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class AuthenticationConfig {

	// Define a maximum age since most browsers use session restoring making session cookies virtual permanent (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)
	@NotNull
	private Duration adminEndCookieDuration = Duration.hours(1);


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

	public static NewCookie expireCookie(String cookieName) {
		return new NewCookie(
				cookieName,
				null,
				"/",
				null,
				null,
				0,
				false
		);
	}

}
