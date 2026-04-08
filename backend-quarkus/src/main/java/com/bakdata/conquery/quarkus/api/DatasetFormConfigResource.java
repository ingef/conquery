package com.bakdata.conquery.quarkus.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Path("/api/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetFormConfigResource {

	@Inject
	DatasetService datasetService;

	@Inject
	FormConfigService formConfigService;

	@Inject
	Instance<SecurityIdentity> identity;

	@POST
	@Path("/{datasetId}/form-configs")
	@Operation(
			summary = "Create a form configuration",
			description = "Creates a form configuration for the dataset and returns its id."
	)
	public PostFormConfigResponse postConfig(
			@PathParam("datasetId") String datasetId,
			@Valid @NotNull FormConfigCreatePayload payload
	) {
		datasetService.requireDataset(datasetId);
		return formConfigService.create(datasetId, payload, SecurityIdentityUtil.resolveUserName(identity));
	}

	@GET
	@Path("/{datasetId}/form-configs")
	@Operation(
			summary = "List form configurations",
			description = "Returns form configuration metadata for the dataset."
	)
	public List<FormConfigOverviewResponse> getConfigs(
			@PathParam("datasetId") String datasetId,
			@QueryParam("formType") Set<String> formType
	) {
		datasetService.requireDataset(datasetId);
		return formConfigService.list(datasetId, formType, SecurityIdentityUtil.resolveUserName(identity));
	}

	public static final class FormConfigCreatePayload {
		@NotBlank
		public String formType;

		public String label;

		@NotNull
		public List<@NotBlank String> tags = List.of();

		@NotNull
		@Schema(
				description = "Opaque form values payload saved by the frontend.",
				implementation = Object.class
		)
		public Map<String, Object> values;
	}

	public record PostFormConfigResponse(
			String id
	) {
	}

	public record FormConfigOverviewResponse(
			String id,
			String formType,
			String label,
			List<String> tags,
			String ownerName,
			String createdAt,
			boolean own,
			boolean shared,
			boolean system
	) {
	}
}
