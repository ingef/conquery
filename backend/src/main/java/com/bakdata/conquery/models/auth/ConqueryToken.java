package com.bakdata.conquery.models.auth;

import org.apache.shiro.authc.AuthenticationToken;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Credentials are wrapped in this class in order to be passed
 * around by shiro.
 */
@RequiredArgsConstructor
public class ConqueryToken implements AuthenticationToken {
	private static final long serialVersionUID = 1L;
	
	/**
	 *  Holds the credentials that verifies a subject.
	 *
	 *  @return The provided credentials (e.g. email address)
	 */
	@Getter
	private final String credentials;
	
	/**
	 * This operation is not supported in this project.
	 *
	 * @return Nothing.
	 * @throws UnsupportedOperationException on use.
	 */
	@Override @JsonIgnore
	public Object getPrincipal() {
		throw new UnsupportedOperationException();
	}
}
