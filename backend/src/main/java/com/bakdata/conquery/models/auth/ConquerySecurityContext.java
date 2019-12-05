package com.bakdata.conquery.models.auth;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConquerySecurityContext implements SecurityContext {

	private final SecurityContext ctx;
	@Getter
	private final ConqueryToken token;

	@Override
	public Principal getUserPrincipal() {
		return ctx.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return ctx.isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return ctx.isSecure();
	}

	@Override
	public String getAuthenticationScheme() {
		return ctx.getAuthenticationScheme();
	}
}
