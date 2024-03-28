package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;

/**
 * Interface that needs to be implemented for authenticating realms in
 * Conquery.
 */
public interface ConqueryAuthenticationRealm extends Realm {

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
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException;

	default User getUserOrThrowUnknownAccount(MetaStorage storage, UserId userId) throws UnknownAccountException {
		final User user = storage.getUser(userId);
		if (user == null) {
			throw new UnknownAccountException("The user id was unknown: " + userId);
		}
		return user;
	}
}
