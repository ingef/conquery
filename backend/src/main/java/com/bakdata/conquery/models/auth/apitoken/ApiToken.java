package com.bakdata.conquery.models.auth.apitoken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.http.util.CharArrayBuffer;
import org.apache.shiro.authc.AuthenticationToken;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.nio.CharBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Container for a sensitive token that allows to use the APIs (see {@link ApiTokenRealm})
 * This object only carries the authenticating part of the token. The authorizing part is within a corresponding
 * {@link ApiTokenData} object.
 *
 * @implNote After the token is processed, its buffer must be cleared to avoid leakage. Conquery has registered the
 * {@link com.bakdata.conquery.io.jackson.serializer.CharArrayBufferSerializer} to post the token once to the user
 * through the API. Be aware, that once this object was serialized, its token is cleared.
 */
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
@Getter
public class ApiToken implements AuthenticationToken {

	// Statics for token hashing
	private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;
	private static final int KEY_LENGTH = 256;
	private static final byte[] SALT = {'s','t','a','t','i','c','_','s','a','l','t'};

	@NotNull
	@NotEmpty
	private final CharArrayBuffer token;

	/**
	 * Id of this token for identification by the user.
	 * This field is only set for tokens that will be serialized.
	 * On incoming tokens this field will be null as it won't be submitted in the
	 * authorization header.
	 */
	@Setter
	private UUID id;

	@Override
	@JsonIgnore
	public CharArrayBuffer getPrincipal() {
		return token;
	}

	@Override
	@JsonIgnore
	public CharArrayBuffer getCredentials() {
		return token;
	}


	public void clear() {
		Arrays.fill(token.buffer(), '\0');
	}



	/**
	 * Hashes only the {@link ApiToken#token}
	 * @param apiToken
	 * @return
	 */
	public ApiTokenHash hashToken(){
		PBEKeySpec spec = new PBEKeySpec(getCredentials().buffer(), SALT, ITERATIONS, KEY_LENGTH);
		SecretKeyFactory f = null;
		try {
			f = SecretKeyFactory.getInstance(ALGORITHM);
			return new ApiTokenHash(f.generateSecret(spec).getEncoded());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("The indicated algorithm was not found", e);
		}
		catch (InvalidKeySpecException e) {
			throw new IllegalStateException("The key specification was invalid", e);
		}
	}
}
