package com.bakdata.conquery.quarkus.storage.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
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
}
