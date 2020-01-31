package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;

public interface UserManageable {
	void addUser(User user, List<CredentialType> credentials, boolean overrideOld);

}
