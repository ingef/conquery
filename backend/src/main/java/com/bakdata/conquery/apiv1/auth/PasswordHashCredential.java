package com.bakdata.conquery.apiv1.auth;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@CPSType(base = CredentialType.class, id = "PASSWORD_HASH")
@Data
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class PasswordHashCredential {

	@NotEmpty
	private final String hash;
}
