package com.bakdata.conquery.models.auth;

import java.util.Optional;

import com.bakdata.conquery.models.auth.entities.UserLike;
import com.bakdata.conquery.models.auth.web.AuthenticationExceptionMapper;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.auth.Authenticator;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * This dropwizard authenticator and the shiro realm are conceptually the same.
 * They authenticate -- but shiro realms can also be used for authorization.
 * We use this single authenticator to set up shiro and forward all requests to
 * shiro, where multiple realms might be configured.
 * We need this authenticator to plug in the security, and hereby shiro, into the AuthFilter.
 */
@Slf4j
public class ConqueryAuthenticator implements Authenticator<AuthenticationToken, UserLike>{
	
	/**
	 * The execeptions thrown by Shiro will be catched by {@link AuthenticationExceptionMapper}.  
	 */
	@Override
	public Optional<UserLike> authenticate(AuthenticationToken token) {
		// Submit the token to Shiro (to all realms that were registered)
		ConqueryAuthenticationInfo info = (ConqueryAuthenticationInfo) SecurityUtils.getSecurityManager().authenticate(token);

		// Extract
		UserLike user = info.getPrincipals().oneByType(UserLike.class);


		// If the user was present, all further authorization can now be performed on the user object
		log.trace("Using user {} for further authorization", user);
		ConqueryMDC.setLocation(user.getId().toString());
		user.setAuthenticationInfo(info);
		return Optional.of(user);
	}

}
