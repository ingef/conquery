package com.bakdata.conquery.models.auth.basic;

public interface UsernamePasswordChecker {

	String checkCredentialsAndCreateJWT(String username, char[] password);

}