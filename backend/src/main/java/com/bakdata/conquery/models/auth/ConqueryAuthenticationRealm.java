package com.bakdata.conquery.models.auth;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * Abstract class that needs to be implemented for authenticating realms in Conquery.
 */
public abstract class ConqueryAuthenticationRealm extends AuthenticatingRealm {
	
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException{
		return doGetConqueryAuthenticationInfo(token);
	}
	
	protected abstract ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException;
	
	/**
	 * Authenticating realms need to be able to extract a token from a request.
	 * How it performs the extraction is implementation dependent.
	 * Anyway the realm should NOT alter the request.
	 * This function is called prior to the authentication process in the {@link DefaultAuthFilter}.
	 * After the token extraction process the Token is resubmitted to the realm from the AuthFilter to
	 * the {@link ConqueryAuthenticator} which dispatches it to shiro.
	 * 
	 * @param request An incoming request that potentially holds a token for the implementing realm.
	 * @return The extracted {@link AuthenticationToken} or <code>null</code> if no token could be parsed.
	 */
	@Nullable
	public abstract AuthenticationToken extractToken(ContainerRequestContext request);
	
}
