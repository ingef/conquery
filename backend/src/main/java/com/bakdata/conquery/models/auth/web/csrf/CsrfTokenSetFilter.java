package com.bakdata.conquery.models.auth.web.csrf;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import com.password4j.Hash;
import com.password4j.PBKDF2Function;
import com.password4j.Password;
import com.password4j.types.Hmac;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;

/**
 * Implementation of the Double-Submit-Cookie Pattern.
 * This filter generates a random token which is injected in to the response.
 * <ul>
 *     <li>In a Set-Cookie header, so that browser requests send the token via cookie back to us</li>
 *     <li>In the response payload. This filter sets a request property, which is eventually provided to freemarker.
 *     Freemarker then writes the token into payload (see base.html.ftl)</li>
 * </ul>
 */
@Slf4j
public class CsrfTokenSetFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String CSRF_COOKIE_NAME = "csrf_token";
	public static final String CSRF_TOKEN_PROPERTY = "csrf_token";
	public static final int TOKEN_LENGTH = 30;

	/**
	 * This needs to be fast, because the hash is computed on every api request and it is only short-lived.
	 */
	private final static PBKDF2Function HASH_FUNCTION = PBKDF2Function.getInstance(Hmac.SHA256, 1000, 256);

	private final Random random = new SecureRandom();

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final String token = RandomStringUtils.random(TOKEN_LENGTH, 0, 0, true, true,
													  null, random
		);
		requestContext.setProperty(CSRF_TOKEN_PROPERTY, token);
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		final String csrfToken = getCsrfTokenProperty(requestContext);

		final String csrfTokenHash = getTokenHash(csrfToken);

		log.trace("Hashed token for cookie. token='{}' hash='{}'", csrfToken, csrfTokenHash);

		responseContext.getHeaders()
					   .add(HttpHeaders.SET_COOKIE, new NewCookie(
							   CSRF_COOKIE_NAME,
							   csrfTokenHash,
							   "/",
							   null,
							   0,
							   null,
							   3600,
							   null,
							   requestContext.getSecurityContext().isSecure(),
							   false
					   ));
	}

	private static String getTokenHash(String csrfToken) {
		final StopWatch stopwatch = log.isTraceEnabled() ? Stopwatch.createStarted() : null;

		final Hash hash = Password.hash(csrfToken).addRandomSalt(32).with(HASH_FUNCTION);
		
		log.trace("Generated token in {}", stopwatch);
		
		final String encodedSalt = Base64.getEncoder().encodeToString(hash.getSaltBytes());

		// Use '_' as join char, because it is not part of the standard base64 encoding (in base64url though)
		return String.join("_", encodedSalt, hash.getResult());
	}

	public static boolean checkHash(String token, String hash) {
		int delimIdx;
		if ((delimIdx = hash.indexOf("_")) == -1 || delimIdx == hash.length() - 1) {
			throw new IllegalArgumentException("The provided hash must be of this form: <salt>_<hashed_salted_token>, was: " + hash);
		}
		final String encodedSalt = hash.substring(0, delimIdx);
		final String saltedHash = hash.substring(delimIdx + 1);

		final byte[] salt = Base64.getDecoder().decode(encodedSalt);
		final StopWatch stopwatch = new StopWatch("Check csrf token");
		stopwatch.start();
		final boolean decision = Password.check(token, saltedHash).addSalt(salt).with(HASH_FUNCTION);
		stopwatch.stop();

		log.trace("Checked token in {}", stopwatch);
		return decision;
	}

	public static String getCsrfTokenProperty(ContainerRequestContext requestContext) {
		return (String) requestContext.getProperty(CSRF_TOKEN_PROPERTY);
	}
}
