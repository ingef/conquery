package com.bakdata.conquery.quarkus.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class QueryStateService {

	private final Map<String, StoredQuery> queriesById = new ConcurrentHashMap<>();

	public DatasetsResource.StartQueryResponse createQuery(String datasetId, QuerySubmissionPayload payload, String ownerName) {
		String queryId = UUID.randomUUID().toString();
		Instant createdAt = Instant.now();
		QueryResource.QueryType queryType = resolveQueryType(payload);
		QueryResource.QueryDefinition definition = toQueryDefinition(payload, queryType);

		StoredQuery stored = new StoredQuery(
				queryId,
				datasetId,
				labelFor(queryId),
				createdAt,
				ownerName,
				definition,
				payload != null ? payload.secondaryId : null,
				containsDates(payload != null ? payload.root : null),
				QueryResource.QueryStatus.NEW,
				List.of(),
				false,
				List.of()
		);

		queriesById.put(queryId, stored);
		return new DatasetsResource.StartQueryResponse(queryId);
	}

	public List<DatasetsResource.QuerySummaryResponse> getDatasetQueries(String datasetId) {
		return queriesById.values()
						 .stream()
						 .filter(query -> query.datasetId().equals(datasetId))
						 .sorted(Comparator.comparing(StoredQuery::createdAt).reversed())
						 .map(this::toSummaryResponse)
						 .toList();
	}

	public QueryResource.QueryResponse getQuery(String queryId) {
		StoredQuery query = requireQuery(queryId);
		return toQueryResponse(query);
	}

	public void cancelQuery(String queryId) {
		StoredQuery query = requireQuery(queryId);
		if (query.status() == QueryResource.QueryStatus.NEW || query.status() == QueryResource.QueryStatus.RUNNING) {
			query.setStatus(QueryResource.QueryStatus.CANCELED);
		}
	}

	public void patchQuery(String queryId, QueryPatch patch) {
		StoredQuery query = requireQuery(queryId);
		if (patch.label() != null && !patch.label().isBlank()) {
			query.setLabel(patch.label());
		}
		if (patch.tags() != null) {
			query.setTags(List.copyOf(patch.tags()));
		}
		if (patch.shared() != null) {
			query.setShared(patch.shared());
		}
		if (patch.groups() != null) {
			query.setGroups(List.copyOf(patch.groups()));
		}
	}

	public void deleteQuery(String queryId) {
		StoredQuery removed = queriesById.remove(queryId);
		if (removed == null) {
			throw new NotFoundException("Unknown query: " + queryId);
		}
	}

	private DatasetsResource.QuerySummaryResponse toSummaryResponse(StoredQuery query) {
		return new DatasetsResource.QuerySummaryResponse(
				query.id(),
				query.label(),
				query.status() == QueryResource.QueryStatus.DONE ? 0L : null,
				query.createdAt().toString(),
				query.tags(),
				true,
				query.ownerName(),
				false,
				List.of(),
				query.shared(),
				false,
				query.definition().type.name(),
				query.secondaryId(),
				query.containsDates()
		);
	}

	private QueryResource.QueryResponse toQueryResponse(StoredQuery query) {
		return switch (query.status()) {
			case RUNNING -> new QueryResource.RunningQueryResponse(
					query.id(),
					query.label(),
					query.createdAt().toString(),
					true,
					query.shared(),
					false,
					query.tags(),
					query.definition(),
					query.secondaryId(),
					query.ownerName(),
					query.groups(),
					false,
					List.of(),
					null
			);
			case DONE -> new QueryResource.DoneQueryResponse(
					query.id(),
					query.label(),
					query.createdAt().toString(),
					true,
					query.shared(),
					false,
					query.tags(),
					query.definition(),
					query.secondaryId(),
					query.ownerName(),
					query.groups(),
					false,
					List.of(),
					0L,
					List.of(),
					query.definition().type,
					0L,
					query.containsDates()
			);
			case FAILED -> new QueryResource.FailedQueryResponse(
					query.id(),
					query.label(),
					query.createdAt().toString(),
					true,
					query.shared(),
					false,
					query.tags(),
					query.definition(),
					query.secondaryId(),
					query.ownerName(),
					query.groups(),
					false,
					List.of(),
					new QueryResource.ErrorResponse("Query failed", "FAILED")
			);
			case CANCELED -> new QueryResource.CanceledQueryResponse(
					query.id(),
					query.label(),
					query.createdAt().toString(),
					true,
					query.shared(),
					false,
					query.tags(),
					query.definition(),
					query.secondaryId(),
					query.ownerName(),
					query.groups(),
					false,
					List.of(),
					new QueryResource.ErrorResponse("Query canceled", "CANCELED")
			);
			case NEW -> new QueryResource.NewQueryResponse(
					query.id(),
					query.label(),
					query.createdAt().toString(),
					true,
					query.shared(),
					false,
					query.tags(),
					query.definition(),
					query.secondaryId(),
					query.ownerName(),
					query.groups(),
					false,
					List.of()
			);
		};
	}

	private QueryResource.QueryDefinition toQueryDefinition(QuerySubmissionPayload payload, QueryResource.QueryType queryType) {
		QuerySubmissionPayload.QueryNode root = payload != null ? payload.root : null;
		return switch (queryType) {
			case SECONDARY_ID_QUERY -> new QueryResource.SecondaryIdQueryDefinition(root, payload != null ? payload.secondaryId : null);
			case CONCEPT_QUERY -> new QueryResource.ConceptQueryDefinition(root);
		};
	}

	private QueryResource.QueryType resolveQueryType(QuerySubmissionPayload payload) {
		if (payload != null && payload.type != null) {
			try {
				return QueryResource.QueryType.valueOf(payload.type.toUpperCase(Locale.ROOT));
			}
			catch (IllegalArgumentException ignored) {
				// Fall through to heuristic handling.
			}
		}
		if (payload != null && payload.secondaryId != null && !payload.secondaryId.isBlank()) {
			return QueryResource.QueryType.SECONDARY_ID_QUERY;
		}
		return QueryResource.QueryType.CONCEPT_QUERY;
	}

	private boolean containsDates(QuerySubmissionPayload.QueryNode node) {
		if (node == null) {
			return false;
		}
		if (node instanceof QuerySubmissionPayload.DateRestrictionNode || node instanceof QuerySubmissionPayload.TemporalNode) {
			return true;
		}
		if (node instanceof QuerySubmissionPayload.AndNode andNode) {
			return anyContainsDates(andNode.children);
		}
		if (node instanceof QuerySubmissionPayload.OrNode orNode) {
			return anyContainsDates(orNode.children);
		}
		if (node instanceof QuerySubmissionPayload.NegationNode negationNode) {
			return containsDates(negationNode.child);
		}
		if (node instanceof QuerySubmissionPayload.DateRestrictionNode dateRestrictionNode) {
			return containsDates(dateRestrictionNode.child);
		}
		return false;
	}

	private boolean anyContainsDates(List<QuerySubmissionPayload.QueryNode> children) {
		if (children == null || children.isEmpty()) {
			return false;
		}
		for (QuerySubmissionPayload.QueryNode child : new ArrayList<>(children)) {
			if (containsDates(child)) {
				return true;
			}
		}
		return false;
	}

	private StoredQuery requireQuery(String queryId) {
		StoredQuery query = queriesById.get(queryId);
		if (query == null) {
			throw new NotFoundException("Unknown query: " + queryId);
		}
		return query;
	}

	private static String labelFor(String queryId) {
		return "Query " + queryId;
	}

	private static final class StoredQuery {
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

		private StoredQuery(
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

	public record QueryPatch(
			String label,
			List<String> tags,
			Boolean shared,
			List<String> groups
	) {
	}
}
