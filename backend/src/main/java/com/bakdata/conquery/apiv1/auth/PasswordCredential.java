package com.bakdata.conquery.apiv1.auth;

import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;

/**
 * Container for holding a plain-text password. This credential type is used by the
 * {@link LocalAuthenticationRealm} and can be used in the {@link AuthorizationConfig}.
 */
@CPSType(base = CredentialType.class, id = "PASSWORD")
public record PasswordCredential(@NotEmpty String password) implements CredentialType {
}
