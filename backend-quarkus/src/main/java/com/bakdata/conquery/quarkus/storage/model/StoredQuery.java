package com.bakdata.conquery.quarkus.storage.model;

import java.time.Instant;
import java.util.List;

import com.bakdata.conquery.quarkus.api.QueryResource;

public class StoredQuery {
	private final String id;
	private final String datasetId;
	private volatile String label;
	private final Instant createdAt;
	private final String ownerName;
	private final QueryResource.QueryDefinition definition;
	private final String secondaryId;
	private final boolean containsDates;
	private volatile QueryResource.QueryStatus status;
	private volatile List<String> tags;
	private volatile boolean shared;
	private volatile List<String> groups;

	public StoredQuery(
			String id,
			String datasetId,
			String label,
			Instant createdAt,
			String ownerName,
			QueryResource.QueryDefinition definition,
			String secondaryId,
			boolean containsDates,
			QueryResource.QueryStatus status,
			List<String> tags,
			boolean shared,
			List<String> groups
	) {
		this.id = id;
		this.datasetId = datasetId;
		this.label = label;
		this.createdAt = createdAt;
		this.ownerName = ownerName;
		this.definition = definition;
		this.secondaryId = secondaryId;
		this.containsDates = containsDates;
		this.status = status;
		this.tags = tags;
		this.shared = shared;
		this.groups = groups;
	}

	public String id() {
		return id;
	}

	public String datasetId() {
		return datasetId;
	}

	public String label() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public String ownerName() {
		return ownerName;
	}

	public QueryResource.QueryDefinition definition() {
		return definition;
	}

	public String secondaryId() {
		return secondaryId;
	}

	public boolean containsDates() {
		return containsDates;
	}

	public QueryResource.QueryStatus status() {
		return status;
	}

	public void setStatus(QueryResource.QueryStatus status) {
		this.status = status;
	}

	public List<String> tags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public boolean shared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public List<String> groups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}
}
