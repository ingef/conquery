package com.bakdata.conquery.models.auth;

import java.util.Collection;
import java.util.Optional;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.AuthenticationExceptionMapper;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.auth.Authenticator;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ConqueryAuthenticator implements Authenticator<AuthenticationToken, User>{
	
	private final MetaStorage storage;

	/**
	 * The execeptions thrown by Shiro will be catched by {@link AuthenticationExceptionMapper}.  
	 */
	@Override
	public Optional<User> authenticate(AuthenticationToken token) {
		// Submit the token to Shiro (to all realms that were registered)
		ConqueryAuthenticationInfo info = (ConqueryAuthenticationInfo) SecurityUtils.getSecurityManager().authenticate(token);
		// All authenticating realms must return a UserId as identifying principal

		// Used the first matching user id
		User user = null;
		Collection<UserId> userIds = info.getPrincipals().byType(UserId.class);
		for(UserId userId : userIds) {
			user = storage.getUser(userId);
			if (user != null) {
				break;
			}
		}
		
		if(user == null) {
			log.trace("None of the provided ids match any user: {}", userIds);
			return Optional.empty();
		}

		// If the user was present, all further authorization can now be performed on the user object
		log.trace("Using user {} for further authorization (provided ids: {})", user, userIds );
		ConqueryMDC.setLocation(user.getId().toString());
		user.setDisplayLogout(info.isDisplayLogout());
		return Optional.ofNullable(user);
	}

}
