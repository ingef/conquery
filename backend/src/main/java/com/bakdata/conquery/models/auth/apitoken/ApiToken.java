package com.bakdata.conquery.models.auth.apitoken;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationToken;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RequiredArgsConstructor
public class ApiToken implements AuthenticationToken {

	@NotNull
	@NotEmpty
	private final char[] token;

	@Override
	public char[] getPrincipal() {
		return token;
	}

	@Override
	public char[] getCredentials() {
		return token;
	}
}
