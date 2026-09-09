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
		String userName = SecurityIdentityUtil.resolveUserName(identity);

		return new MeResponse(
				userName,
				true,
				Map.of("imdb", new PermissionFlags(true, true, true)),
				List.of()
		);
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
