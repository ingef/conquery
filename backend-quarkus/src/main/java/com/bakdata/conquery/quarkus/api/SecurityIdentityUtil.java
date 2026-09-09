package com.bakdata.conquery.quarkus.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;

public final class SecurityIdentityUtil {

	private SecurityIdentityUtil() {
	}

	public static String resolveUserName(Instance<SecurityIdentity> identityInstance) {
		if (identityInstance.isResolvable()) {
			SecurityIdentity identity = identityInstance.get();
			if (identity != null && !identity.isAnonymous() && identity.getPrincipal() != null) {
				String principalName = identity.getPrincipal().getName();
				if (principalName != null && !principalName.isBlank()) {
					return principalName;
				}
			}
		}

		return "anonymous";
	}
}
