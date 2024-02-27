package com.bakdata.conquery.apiv1.auth;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(base = CredentialType.class, id = "PASSWORD_HASH")
public record PasswordHashCredential(@NotEmpty String hash) implements CredentialType {
}
