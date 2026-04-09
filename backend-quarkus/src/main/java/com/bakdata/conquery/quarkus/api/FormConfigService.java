package com.bakdata.conquery.quarkus.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.model.StoredFormConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class FormConfigService {

	@Inject
	FormConfigRepository formConfigRepository;

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
		formConfigRepository.save(stored);
		return new DatasetFormConfigResource.PostFormConfigResponse(id);
	}

	public List<DatasetFormConfigResource.FormConfigOverviewResponse> list(
			String datasetId,
			Set<String> requestedFormTypes,
			String requester
	) {
		return formConfigRepository.listByDataset(datasetId).stream()
						  .filter(config -> requestedFormTypes == null
								  || requestedFormTypes.isEmpty()
								  || requestedFormTypes.contains(config.formType()))
						  .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
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
		formConfigRepository.save(config);
		return toFull(config, requester);
	}

	public void delete(String formConfigId) {
		if (!formConfigRepository.deleteById(formConfigId)) {
			throw new NotFoundException("Unknown form-config: " + formConfigId);
		}
	}

	private DatasetFormConfigResource.FormConfigOverviewResponse toOverview(StoredFormConfig config, String requester) {
		return new DatasetFormConfigResource.FormConfigOverviewResponse(
				config.id(),
				config.formType(),
				config.label(),
				config.tags(),
				config.ownerName(),
				config.createdAt().toString(),
				config.ownerName().equals(requester),
				!config.groups().isEmpty(),
				config.system()
		);
	}

	private FormConfigResource.FormConfigFullResponse toFull(StoredFormConfig config, String requester) {
		return new FormConfigResource.FormConfigFullResponse(
				config.id(),
				config.formType(),
				config.label(),
				config.tags(),
				config.ownerName(),
				config.createdAt().toString(),
				config.ownerName().equals(requester),
				!config.groups().isEmpty(),
				config.system(),
				config.groups(),
				config.values()
		);
	}

	private StoredFormConfig require(String formConfigId) {
		return formConfigRepository.findById(formConfigId).orElseThrow(() -> new NotFoundException("Unknown form-config: " + formConfigId));
	}
}
