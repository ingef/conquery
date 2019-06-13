package com.bakdata.eva.models.auth;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.shiro.authc.AuthenticationException;

import com.bakdata.eva.models.auth.IngefCredentials.IngefCredentialsBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import io.dropwizard.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class IngefCredentialParser{

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String ENCRYPTION = "AES";
	private static final String SP_EMAIL = "sp_email";
	private static final String SP_KASSE_IK_0 = "sp_kasse_ik_0";
	private static final String SP_PORTAL_USERNAME = "sp_portal_username";
	private static final String SP_TIMESTAMP = "sp_timestamp";
	private static final String[] REQUIRED_PARAMETERS = {SP_EMAIL, SP_KASSE_IK_0, SP_TIMESTAMP,
		SP_PORTAL_USERNAME};
	private final Map<String, BiFunction<IngefCredentialsBuilder, String, Boolean>> parameterHandlers = ImmutableMap.<String, BiFunction<IngefCredentialsBuilder, String, Boolean>>builder()
		.put(SP_EMAIL, this::handleEmail)
		.put(SP_KASSE_IK_0, this::handleKasseIk)
		.put(SP_PORTAL_USERNAME, this::handleUsername)
		.put(SP_TIMESTAMP, this::handleTimestamp)
		.build();

	private Duration expiryTime = null;
	@NotNull
	private String secret;

	public IngefCredentials parse(String credentials) throws AuthenticationException {
		return parseUnchecked(credentials);
	}

	private IngefCredentials parseUnchecked(String credentials) throws AuthenticationException {
		List<NameValuePair> urlParameters = parseURLParameters(decrypt(credentials));
		return parse(urlParameters);
	}

	private List<NameValuePair> parseURLParameters(String decrypted) {
		List<NameValuePair> pairs = URLEncodedUtils.parse(decrypted, CHARSET);
		log.info("Parsed the authorization token {}", pairs);
		return pairs;
	}

	private IngefCredentials parse(List<NameValuePair> params) throws AuthenticationException {
		Set<String> requiredParameters = Sets.newHashSet(REQUIRED_PARAMETERS);
		IngefCredentialsBuilder builder = IngefCredentials.builder();
		for (NameValuePair param : params) {
			parseParameter(param, builder)
				.ifPresent(requiredParameters::remove);
		}
		if (requiredParameters.isEmpty()) {
			return builder.build();
		}
		throw new AuthenticationException("Credentials invalid. Missing parameters " + requiredParameters);
	}

	private Optional<String> parseParameter(NameValuePair param, IngefCredentialsBuilder builder) {
		String value = StringUtils.trim(param.getValue());
		String name = param.getName();
		boolean valid = parseParameter(builder, value, name);
		return valid ? Optional.of(name) : Optional.empty();
	}

	private boolean parseParameter(IngefCredentialsBuilder builder, String value, String name) {
		return Optional.ofNullable(parameterHandlers.get(name))
			.map(f -> f.apply(builder, value))
			.orElse(false);
	}

	private boolean handleEmail(IngefCredentialsBuilder builder, String value) {
		if (StringUtils.isNotEmpty(value)) {
			builder.email(value);
			return true;
		}
		log.warn("Email not present in credential");
		return false;
	}

	private boolean handleKasseIk(IngefCredentialsBuilder builder, String value) {
		try {
			builder.company(value);
			return true;
		} catch (NumberFormatException e) {
			log.warn("The value of KasseIk could not be parsed as an integer: {}", value);
			return false;
		}
	}

	private boolean handleUsername(IngefCredentialsBuilder builder, String value) {
		if (StringUtils.isNotEmpty(value)) {
			builder.name(value);
			return true;
		}
		log.warn("Username not present in credential");
		return false;
	}

	private boolean handleTimestamp(IngefCredentialsBuilder builder, String value) {
		if (value == null) {
			log.warn("Timestamp not present in credential");
			return false;
		}
		try {
			LocalDateTime timestamp = LocalDateTime.parse(value);
			builder.validUntil(toValidUntil(timestamp));
			return true;
		} catch (DateTimeParseException e) {
			log.warn("Timestamp {} could not be parsed", value);
			return false;
		}
	}

	private LocalDateTime toValidUntil(LocalDateTime timestamp) {
		return Optional.ofNullable(expiryTime)
			.map(Duration::toNanoseconds)
			.map(timestamp::plusNanos)
			.orElse(LocalDateTime.MAX);
	}

	private String decrypt(String data) throws AuthenticationException {
		Cipher cipher;
		try {
			byte[] encrypted = Base64.getDecoder().decode(data);
			cipher = createCipher();
			byte[] decrypted = cipher.doFinal(encrypted);
			return new String(decrypted, CHARSET);
		} catch (GeneralSecurityException|NullPointerException e) {
			throw new AuthenticationException("Unable to decrypt credentials");
		}
	}

	private Cipher createCipher() throws GeneralSecurityException {
		Key key = new SecretKeySpec(secret.getBytes(), ENCRYPTION);
		Cipher cipher = Cipher.getInstance(ENCRYPTION);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher;
	}
}
