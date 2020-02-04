package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public interface UserManageable {

	boolean addUser(User user, List<CredentialType> credentials);

	boolean updateUser(User user, List<CredentialType> credentials);

	boolean removeUser(User user);

	List<UserId> getAllUsers();
}
