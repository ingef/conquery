package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;

@CPSType(base = CredentialType.class, id = "PASSWORD")
@Getter
public class PasswordCredential implements CredentialType {
	private String password;
}
