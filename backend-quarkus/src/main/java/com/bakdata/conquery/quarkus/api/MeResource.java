package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {
	@Inject
	Instance<SecurityIdentity> identity;

	@GET
	public MeResponse me() {
		String userName = resolveUserName(identity);

		return new MeResponse(
				userName,
				true,
				Map.of(),
				List.of()
		);
	}

	private static String resolveUserName(Instance<SecurityIdentity> identityInstance) {
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

	public record MeResponse(
			String userName,
			boolean hideLogoutButton,
			Map<String, PermissionFlags> datasetAbilities,
			List<GroupInfo> groups
	) {
	}

	public record PermissionFlags(
			boolean canUpload,
			boolean canViewEntityPreview,
			boolean canViewQueryPreview
	) {
	}

	public record GroupInfo(
			String id,
			String label
	) {
	}
}
