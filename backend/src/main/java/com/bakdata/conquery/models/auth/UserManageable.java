package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

/**
 * Basic functionality for a realm to manage users and interface with {@link Conquery}. 
 */
public interface UserManageable {

	/**
	 * Add a user to an authenticating realm. The realm is responsible for picking the appropriate {@link CredentialType} from the argument.
	 * If not fitting type was found the user should not be added.
	 *
	 * @param credentials A List of credentials that are provided by the user.
	 * @param userId
	 * @return True upon successful adding of the user. False if the user could not be added or was already present.
	 */
	boolean addUser(UserId userId, CredentialType credential);

	
	/**
	 * Similar to {@link UserManageable#addUser(User, List)} but if the user already existed it is overridden, when a fitting {@link CredentialType} was found.
	 */
	boolean updateUser(UserId user, CredentialType credential);

	/**
	 * Removes a user from the realm only but not from the local permission storage (i.e. {@link MetaStorage}).
	 */
	boolean removeUser(UserId user);

	/**
	 * Returns a list of all users managed by the authenticating realm as {@link UserId}s.
	 */
	List<UserId> getAllUsers();
}
