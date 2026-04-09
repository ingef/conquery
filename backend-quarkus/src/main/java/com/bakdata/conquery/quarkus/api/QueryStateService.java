package com.bakdata.conquery.quarkus.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bakdata.conquery.quarkus.storage.QueryRepository;
import com.bakdata.conquery.quarkus.storage.model.StoredQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class QueryStateService {

	@Inject
	QueryRepository queryRepository;

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
				extractSecondaryId(payload),
				containsDates(extractRoot(payload)),
				QueryResource.QueryStatus.NEW,
				List.of(),
				false,
				List.of()
		);

		queryRepository.save(stored);
		return new DatasetsResource.StartQueryResponse(queryId);
	}

	public List<DatasetsResource.QuerySummaryResponse> getDatasetQueries(String datasetId) {
		return queryRepository.listByDataset(datasetId).stream()
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
		if (!queryRepository.deleteById(queryId)) {
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
		QuerySubmissionPayload.QueryNode root = extractRoot(payload);
		return switch (queryType) {
			case SECONDARY_ID_QUERY -> new QueryResource.SecondaryIdQueryDefinition(root, extractSecondaryId(payload));
			case CONCEPT_QUERY -> new QueryResource.ConceptQueryDefinition(root);
		};
	}

	private QueryResource.QueryType resolveQueryType(QuerySubmissionPayload payload) {
		if (payload instanceof QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload) {
			return QueryResource.QueryType.SECONDARY_ID_QUERY;
		}
		if (payload instanceof QuerySubmissionPayload.ConceptQuerySubmissionPayload) {
			return QueryResource.QueryType.CONCEPT_QUERY;
		}
		if (payload != null && payload.type != null) {
			try {
				return QueryResource.QueryType.valueOf(payload.type.toUpperCase(Locale.ROOT));
			}
			catch (IllegalArgumentException ignored) {
				// Fall through to heuristic handling.
			}
		}
		String secondaryId = extractSecondaryId(payload);
		if (secondaryId != null && !secondaryId.isBlank()) {
			return QueryResource.QueryType.SECONDARY_ID_QUERY;
		}
		return QueryResource.QueryType.CONCEPT_QUERY;
	}

	private String extractSecondaryId(QuerySubmissionPayload payload) {
		if (payload instanceof QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload secondaryIdPayload) {
			return secondaryIdPayload.secondaryId;
		}
		return null;
	}

	private QuerySubmissionPayload.QueryNode extractRoot(QuerySubmissionPayload payload) {
		if (payload instanceof QuerySubmissionPayload.ConceptQuerySubmissionPayload conceptPayload) {
			return conceptPayload.root;
		}
		if (payload instanceof QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload secondaryIdPayload) {
			return secondaryIdPayload.root;
		}
		return null;
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
		return queryRepository.findById(queryId).orElseThrow(() -> new NotFoundException("Unknown query: " + queryId));
	}

	private static String labelFor(String queryId) {
		return "Query " + queryId;
	}

	public record QueryPatch(
			String label,
			List<String> tags,
			Boolean shared,
			List<String> groups
	) {
	}
}
