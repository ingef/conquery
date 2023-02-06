package com.bakdata.conquery.apiv1.auth;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Container for holding a password. This credential type is used by the
 * {@link LocalAuthenticationRealm} and can be used in the {@link AuthorizationConfig}. 
 */
@CPSType(base = CredentialType.class, id = "PASSWORD")
@Data
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
@AllArgsConstructor
public class PasswordCredential implements CredentialType {

	@NotEmpty
	private char[] password;
}
