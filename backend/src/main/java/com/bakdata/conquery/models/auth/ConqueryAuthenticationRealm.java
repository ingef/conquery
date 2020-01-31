package com.bakdata.conquery.models.auth;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;

/**
 * Interface that is to be used on classes that extend {@link Realm} in Conquery.
 */
public interface ConqueryAuthenticationRealm {
	@Nullable
	AuthenticationToken extractToken(ContainerRequestContext request);
	
}
