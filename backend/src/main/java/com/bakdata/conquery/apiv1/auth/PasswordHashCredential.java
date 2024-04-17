package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.io.cps.CPSType;
import jakarta.validation.constraints.NotEmpty;

@CPSType(base = CredentialType.class, id = "PASSWORD_HASH")
public record PasswordHashCredential(@NotEmpty String hash) implements CredentialType {
}
