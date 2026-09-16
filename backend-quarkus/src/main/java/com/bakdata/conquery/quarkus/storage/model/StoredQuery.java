package com.bakdata.conquery.quarkus.storage.model;

import java.time.Instant;
import java.util.List;

import com.bakdata.conquery.quarkus.api.QueryResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StoredQuery {
	private final String id;
	private final String datasetId;
	private String label;
	private final Instant createdAt;
	private final String ownerName;
	private final QueryResource.QueryDefinition definition;
	private final String secondaryId;
	private final boolean containsDates;
	private QueryResource.QueryStatus status;
	private List<String> tags;
	private boolean shared;
	private List<String> groups;
}
