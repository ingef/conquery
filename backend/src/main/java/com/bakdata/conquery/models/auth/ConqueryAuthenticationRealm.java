package com.bakdata.conquery.models.auth;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.util.Destroyable;

/**
 * Abstract class that needs to be implemented for authenticating realms in
 * Conquery.
 */
@RequiredArgsConstructor
public abstract class ConqueryAuthenticationRealm extends AuthenticatingRealm implements Destroyable {

	protected final MetaStorage storage;

	@Override
	protected final AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		return doGetConqueryAuthenticationInfo(token);
	}

	/**
	 * Method that all authenticating realms in {@link Conquery} need to implement.
	 * It is a stricter version of
	 * {@link AuthenticatingRealm#doGetAuthenticationInfo}, which enforces a return
	 * type of {@link ConqueryAuthenticationInfo}.
	 * 
	 * @param token A token that the realm previously extracted from a request.
	 * @return An {@link ConqueryAuthenticationInfo} containing the UserId of the user that caused the request or {@code null}, which means that no account could be associated with the specified token.
	 * @throws AuthenticationException Upon failed authentication.
	 */
	protected abstract ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException;

	@Override
	public void destroy() throws Exception {
		// Might be implemented if the realm needs to release resources
	}

	protected User getUserOrThrowUnknownAccount(UserId userId) throws UnknownAccountException {
		final User user = storage.getUser(userId);
		if (user == null) {
			throw new UnknownAccountException("The user id was unknown: " + userId);
		}
		return user;
	}
}
