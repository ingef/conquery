package com.bakdata.conquery.models.auth;

import java.util.Optional;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.io.ConqueryMDC;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import lombok.extern.slf4j.Slf4j;

/**
 * This dropwizard authenticator and the shiro realm are conceptually the same.
 * They authenticate -- but shiro realms can also be used for authorization.
 * We use this single authenticator to set up shiro and forward all requests to
 * shiro, where multiple realms might be configured.
 * We need this authenticator to plug in the security, and hereby shiro, into the AuthFilter.
 */
@Slf4j
public class ConqueryAuthenticator implements Authenticator<ConqueryToken, User>{
	
	private final MasterMetaStorage storage;
	
	public ConqueryAuthenticator(MasterMetaStorage storage, Realm realm) {
		this.storage = storage;
		
		SecurityManager securityManager = new DefaultSecurityManager(realm);
		SecurityUtils.setSecurityManager(securityManager);
		log.debug("Security manager registered");
	}

	@Override
	public Optional<User> authenticate(ConqueryToken token) throws AuthenticationException {
		
		AuthenticationInfo info = SecurityUtils.getSecurityManager().authenticate(token);
		UserId userId = (UserId)info.getPrincipals().getPrimaryPrincipal();

		User user = storage.getUser(userId);
		
		ConqueryMDC.setLocation(user.getId().toString());
		return Optional.ofNullable(user);
	}

}
