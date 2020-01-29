package com.bakdata.conquery.models.auth;


public interface UserManageable {
	void addUser(String username, String password, boolean overrideOld);

}
