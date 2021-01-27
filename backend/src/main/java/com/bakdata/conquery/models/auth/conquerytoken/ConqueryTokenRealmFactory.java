package com.bakdata.conquery.models.auth.conquerytoken;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.JWTokenHandler;

public class ConqueryTokenRealmFactory implements AuthenticationConfig {
	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(JWTokenHandler::extractToken);

		return new ConqueryTokenRealm(managerNode.getStorage());
	}
}
