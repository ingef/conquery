package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@CPSType(base = CredentialType.class, id = "PASSWORD")
@Getter
@NoArgsConstructor(onConstructor = @__({@JsonCreator}))
@AllArgsConstructor
public class PasswordCredential implements CredentialType {
	private String password;
}
