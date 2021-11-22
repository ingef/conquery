package com.bakdata.conquery.models.auth.apitoken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.CharArrayBuffer;
import org.apache.shiro.authc.AuthenticationToken;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.nio.CharBuffer;
import java.util.Arrays;

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

	@NotNull
	@NotEmpty
	private final CharArrayBuffer token;

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
}
