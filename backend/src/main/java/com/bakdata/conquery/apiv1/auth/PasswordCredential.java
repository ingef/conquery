package com.bakdata.conquery.apiv1.auth;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import lombok.Data;

/**
 * Container for holding a password. This credential type is used by the
 * {@link LocalAuthenticationRealm}.
 */
@CPSType(base = CredentialType.class, id = "PASSWORD")
@Data
public class PasswordCredential implements CredentialType {

	private final char[] password;
}
