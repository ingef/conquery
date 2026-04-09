package com.bakdata.conquery.quarkus.storage.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoredFormConfig {
	private final String id;
	private final String datasetId;
	private final String formType;
	private String label;
	private List<String> tags;
	private final String ownerName;
	private final Instant createdAt;
	private final boolean system;
	private List<String> groups;
	private Map<String, Object> values;

	public StoredFormConfig(
			String id,
			String datasetId,
			String formType,
			String label,
			List<String> tags,
			String ownerName,
			Instant createdAt,
			boolean system,
			List<String> groups,
			Map<String, Object> values
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

	public String id() {
		return id;
	}

	public String datasetId() {
		return datasetId;
	}

	public String formType() {
		return formType;
	}

	public String label() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<String> tags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String ownerName() {
		return ownerName;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public boolean system() {
		return system;
	}

	public List<String> groups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public Map<String, Object> values() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}
}
