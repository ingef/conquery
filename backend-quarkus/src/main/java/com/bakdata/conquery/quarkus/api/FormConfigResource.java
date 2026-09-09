package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.services.FormConfigService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Path("/api/form-configs")
@Produces(MediaType.APPLICATION_JSON)
public class FormConfigResource {

	@Inject
    FormConfigService formConfigService;

	@Inject
	Instance<SecurityIdentity> identity;

	@GET
	@Path("/{formConfigId}")
	@Operation(
			summary = "Get a form configuration",
			description = "Returns full form configuration metadata and values."
	)
	public FormConfigFullResponse getConfig(@PathParam("formConfigId") String formConfigId) {
		return formConfigService.get(formConfigId, SecurityIdentityUtil.resolveUserName(identity));
	}

	@PATCH
	@Path("/{formConfigId}")
	@Operation(
			summary = "Patch a form configuration",
			description = "Patches mutable form configuration fields."
	)
	public FormConfigFullResponse patchConfig(
			@PathParam("formConfigId") String formConfigId,
			@Valid FormConfigPatchPayload payload
	) {
		return formConfigService.patch(formConfigId, payload, SecurityIdentityUtil.resolveUserName(identity));
	}

	@DELETE
	@Path("/{formConfigId}")
	@Operation(
			summary = "Delete a form configuration",
			description = "Deletes a stored form configuration."
	)
	public void deleteConfig(@PathParam("formConfigId") String formConfigId) {
		formConfigService.delete(formConfigId);
	}

	public static final class FormConfigPatchPayload {
		public String label;
		public List<@NotBlank String> tags;
		public List<@NotBlank String> groups;

		@Schema(
				description = "Opaque form values payload saved by the frontend.",
				implementation = Object.class
		)
		public Map<String, Object> values;
	}

	public record FormConfigFullResponse(
			String id,
			String formType,
			String label,
			List<String> tags,
			String ownerName,
			String createdAt,
			boolean own,
			boolean shared,
			boolean system,
			List<String> groups,
			@Schema(
					description = "Opaque form values payload saved by the frontend.",
					implementation = Object.class
			)
			Map<String, Object> values
	) {
	}
}
