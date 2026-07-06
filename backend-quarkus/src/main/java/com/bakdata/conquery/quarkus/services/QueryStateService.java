package com.bakdata.conquery.quarkus.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bakdata.conquery.quarkus.api.DatasetsResource;
import com.bakdata.conquery.quarkus.api.QueryResource;
import com.bakdata.conquery.quarkus.api.QuerySubmissionPayload;
import com.bakdata.conquery.quarkus.storage.meta.ManagerMetaStorage;
import com.bakdata.conquery.quarkus.storage.model.StoredQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class QueryStateService {

	@Inject
	ManagerMetaStorage metaStorage;

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

		metaStorage.queries().save(stored);
		return new DatasetsResource.StartQueryResponse(queryId);
	}

	public List<DatasetsResource.QuerySummaryResponse> getDatasetQueries(String datasetId) {
		return metaStorage.queries().listByDataset(datasetId).stream()
						 .sorted(Comparator.comparing(StoredQuery::getCreatedAt).reversed())
						 .map(this::toSummaryResponse)
						 .toList();
	}

	public QueryResource.QueryResponse getQuery(String queryId) {
		StoredQuery query = requireQuery(queryId);
		return toQueryResponse(query);
	}

	public void cancelQuery(String queryId) {
		StoredQuery query = requireQuery(queryId);
		if (query.getStatus() == QueryResource.QueryStatus.NEW || query.getStatus() == QueryResource.QueryStatus.RUNNING) {
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
		if (!metaStorage.queries().deleteById(queryId)) {
			throw new NotFoundException("Unknown query: " + queryId);
		}
	}

	private DatasetsResource.QuerySummaryResponse toSummaryResponse(StoredQuery query) {
		return new DatasetsResource.QuerySummaryResponse(
				query.getId(),
				query.getLabel(),
				query.getStatus() == QueryResource.QueryStatus.DONE ? 0L : null,
				query.getCreatedAt().toString(),
				query.getTags(),
				true,
				query.getOwnerName(),
				false,
				List.of(),
				query.isShared(),
				false,
				query.getDefinition().type.name(),
				query.getSecondaryId(),
				query.isContainsDates()
		);
	}

	private QueryResource.QueryResponse toQueryResponse(StoredQuery query) {
		return switch (query.getStatus()) {
			case RUNNING -> new QueryResource.RunningQueryResponse(
					query.getId(),
					query.getLabel(),
					query.getCreatedAt().toString(),
					true,
					query.isShared(),
					false,
					query.getTags(),
					query.getDefinition(),
					query.getSecondaryId(),
					query.getOwnerName(),
					query.getGroups(),
					false,
					List.of(),
					null
			);
			case DONE -> new QueryResource.DoneQueryResponse(
					query.getId(),
					query.getLabel(),
					query.getCreatedAt().toString(),
					true,
					query.isShared(),
					false,
					query.getTags(),
					query.getDefinition(),
					query.getSecondaryId(),
					query.getOwnerName(),
					query.getGroups(),
					false,
					List.of(),
					0L,
					List.of(),
					query.getDefinition().type,
					0L,
					query.isContainsDates()
			);
			case FAILED -> new QueryResource.FailedQueryResponse(
					query.getId(),
					query.getLabel(),
					query.getCreatedAt().toString(),
					true,
					query.isShared(),
					false,
					query.getTags(),
					query.getDefinition(),
					query.getSecondaryId(),
					query.getOwnerName(),
					query.getGroups(),
					false,
					List.of(),
					new QueryResource.ErrorResponse("Query failed", "FAILED")
			);
			case CANCELED -> new QueryResource.CanceledQueryResponse(
					query.getId(),
					query.getLabel(),
					query.getCreatedAt().toString(),
					true,
					query.isShared(),
					false,
					query.getTags(),
					query.getDefinition(),
					query.getSecondaryId(),
					query.getOwnerName(),
					query.getGroups(),
					false,
					List.of(),
					new QueryResource.ErrorResponse("Query canceled", "CANCELED")
			);
			case NEW -> new QueryResource.NewQueryResponse(
					query.getId(),
					query.getLabel(),
					query.getCreatedAt().toString(),
					true,
					query.isShared(),
					false,
					query.getTags(),
					query.getDefinition(),
					query.getSecondaryId(),
					query.getOwnerName(),
					query.getGroups(),
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
        return switch (node) {
            case QuerySubmissionPayload.AndNode andNode -> anyContainsDates(andNode.children);
            case QuerySubmissionPayload.OrNode orNode -> anyContainsDates(orNode.children);
            case QuerySubmissionPayload.NegationNode negationNode -> containsDates(negationNode.child);
            default -> false;
        };
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
		return metaStorage.queries().findById(queryId).orElseThrow(() -> new NotFoundException("Unknown query: " + queryId));
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
