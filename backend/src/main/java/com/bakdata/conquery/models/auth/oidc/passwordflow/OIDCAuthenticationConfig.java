package com.bakdata.conquery.models.auth.oidc.passwordflow;

import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

public interface OIDCAuthenticationConfig extends AuthenticationConfig{
	
	
	public String getTokenEndpoint();
	
	public String getIntrospectionEndpoint();
	
	public ClientAuthentication getClientAuthentication();
}
