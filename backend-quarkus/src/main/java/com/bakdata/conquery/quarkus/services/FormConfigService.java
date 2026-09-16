package com.bakdata.conquery.quarkus.services;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.quarkus.api.DatasetFormConfigResource;
import com.bakdata.conquery.quarkus.api.FormConfigResource;
import com.bakdata.conquery.quarkus.storage.meta.ManagerMetaStorage;
import com.bakdata.conquery.quarkus.storage.model.StoredFormConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class FormConfigService {

	@Inject
	ManagerMetaStorage metaStorage;

	public DatasetFormConfigResource.PostFormConfigResponse create(
			String datasetId,
			DatasetFormConfigResource.FormConfigCreatePayload payload,
			String ownerName
	) {
		String id = datasetId + ":" + payload.formType + ":" + UUID.randomUUID();
		StoredFormConfig stored = new StoredFormConfig(
				id,
				datasetId,
				payload.formType,
				payload.label,
				List.copyOf(payload.tags),
				ownerName,
				Instant.now(),
				false,
				List.of(),
				payload.values
		);
		metaStorage.formConfigs().save(stored);
		return new DatasetFormConfigResource.PostFormConfigResponse(id);
	}

	public List<DatasetFormConfigResource.FormConfigOverviewResponse> list(
			String datasetId,
			Set<String> requestedFormTypes,
			String requester
	) {
		return metaStorage.formConfigs().listByDataset(datasetId).stream()
						  .filter(config -> requestedFormTypes == null
								  || requestedFormTypes.isEmpty()
								  || requestedFormTypes.contains(config.getFormType()))
						  .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
						  .map(config -> toOverview(config, requester))
						  .toList();
	}

	public FormConfigResource.FormConfigFullResponse get(String formConfigId, String requester) {
		return toFull(require(formConfigId), requester);
	}

	public FormConfigResource.FormConfigFullResponse patch(
			String formConfigId,
			FormConfigResource.FormConfigPatchPayload payload,
			String requester
	) {
		StoredFormConfig config = require(formConfigId);
		if (payload.label != null) {
			config.setLabel(payload.label);
		}
		if (payload.tags != null) {
			config.setTags(List.copyOf(payload.tags));
		}
		if (payload.groups != null) {
			config.setGroups(List.copyOf(payload.groups));
		}
		if (payload.values != null) {
			config.setValues(payload.values);
		}
		metaStorage.formConfigs().save(config);
		return toFull(config, requester);
	}

	public void delete(String formConfigId) {
		if (!metaStorage.formConfigs().deleteById(formConfigId)) {
			throw new NotFoundException("Unknown form-config: " + formConfigId);
		}
	}

	private DatasetFormConfigResource.FormConfigOverviewResponse toOverview(StoredFormConfig config, String requester) {
		return new DatasetFormConfigResource.FormConfigOverviewResponse(
				config.getId(),
				config.getFormType(),
				config.getLabel(),
				config.getTags(),
				config.getOwnerName(),
				config.getCreatedAt().toString(),
				config.getOwnerName().equals(requester),
				!config.getGroups().isEmpty(),
				config.isSystem()
		);
	}

	private FormConfigResource.FormConfigFullResponse toFull(StoredFormConfig config, String requester) {
		return new FormConfigResource.FormConfigFullResponse(
				config.getId(),
				config.getFormType(),
				config.getLabel(),
				config.getTags(),
				config.getOwnerName(),
				config.getCreatedAt().toString(),
				config.getOwnerName().equals(requester),
				!config.getGroups().isEmpty(),
				config.isSystem(),
				config.getGroups(),
				config.getValues()
		);
	}

	private StoredFormConfig require(String formConfigId) {
		return metaStorage.formConfigs().findById(formConfigId).orElseThrow(() -> new NotFoundException("Unknown form-config: " + formConfigId));
	}
}
