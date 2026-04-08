package com.bakdata.conquery.quarkus.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class FormConfigService {

	private final Map<String, StoredFormConfig> configsById = new ConcurrentHashMap<>();

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
		configsById.put(id, stored);
		return new DatasetFormConfigResource.PostFormConfigResponse(id);
	}

	public List<DatasetFormConfigResource.FormConfigOverviewResponse> list(
			String datasetId,
			Set<String> requestedFormTypes,
			String requester
	) {
		return configsById.values().stream()
						  .filter(config -> config.datasetId.equals(datasetId))
						  .filter(config -> requestedFormTypes == null
								  || requestedFormTypes.isEmpty()
								  || requestedFormTypes.contains(config.formType))
						  .sorted((left, right) -> right.createdAt.compareTo(left.createdAt))
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
			config.label = payload.label;
		}
		if (payload.tags != null) {
			config.tags = List.copyOf(payload.tags);
		}
		if (payload.groups != null) {
			config.groups = List.copyOf(payload.groups);
		}
		if (payload.values != null) {
			config.values = payload.values;
		}
		return toFull(config, requester);
	}

	public void delete(String formConfigId) {
		StoredFormConfig removed = configsById.remove(formConfigId);
		if (removed == null) {
			throw new NotFoundException("Unknown form-config: " + formConfigId);
		}
	}

	private DatasetFormConfigResource.FormConfigOverviewResponse toOverview(StoredFormConfig config, String requester) {
		return new DatasetFormConfigResource.FormConfigOverviewResponse(
				config.id,
				config.formType,
				config.label,
				config.tags,
				config.ownerName,
				config.createdAt.toString(),
				config.ownerName.equals(requester),
				!config.groups.isEmpty(),
				config.system
		);
	}

	private FormConfigResource.FormConfigFullResponse toFull(StoredFormConfig config, String requester) {
		return new FormConfigResource.FormConfigFullResponse(
				config.id,
				config.formType,
				config.label,
				config.tags,
				config.ownerName,
				config.createdAt.toString(),
				config.ownerName.equals(requester),
				!config.groups.isEmpty(),
				config.system,
				config.groups,
				config.values
		);
	}

	private StoredFormConfig require(String formConfigId) {
		StoredFormConfig config = configsById.get(formConfigId);
		if (config == null) {
			throw new NotFoundException("Unknown form-config: " + formConfigId);
		}
		return config;
	}

	private static final class StoredFormConfig {
		private final String id;
		private final String datasetId;
		private final String formType;
		private String label;
		private List<String> tags;
		private final String ownerName;
		private final Instant createdAt;
		private final boolean system;
		private List<String> groups;
		private Object values;

		private StoredFormConfig(
				String id,
				String datasetId,
				String formType,
				String label,
				List<String> tags,
				String ownerName,
				Instant createdAt,
				boolean system,
				List<String> groups,
				Object values
		) {
			this.id = id;
			this.datasetId = datasetId;
			this.formType = formType;
			this.label = label;
			this.tags = new ArrayList<>(tags);
			this.ownerName = ownerName;
			this.createdAt = createdAt;
			this.system = system;
			this.groups = new ArrayList<>(groups);
			this.values = values;
		}
	}
}
