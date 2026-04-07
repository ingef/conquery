package com.bakdata.conquery.quarkus.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Path("/api/queries")
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {
	@Inject
	QueryStateService queryStateService;

	@GET
	@Path("/{queryId}")
	@Operation(
			summary = "Get a query by id",
			description = "Returns the current query execution status and metadata."
	)
	public QueryResponse getQuery(@PathParam("queryId") String queryId) {
		return queryStateService.getQuery(queryId);
	}

	@POST
	@Path("/{queryId}/cancel")
	@Operation(
			summary = "Cancel a query",
			description = "Cancels a running query."
	)
	public void cancelQuery(@PathParam("queryId") String queryId) {
		queryStateService.cancelQuery(queryId);
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "status", visible = true)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = NewQueryResponse.class, name = "NEW"),
			@JsonSubTypes.Type(value = RunningQueryResponse.class, name = "RUNNING"),
			@JsonSubTypes.Type(value = DoneQueryResponse.class, name = "DONE"),
			@JsonSubTypes.Type(value = FailedQueryResponse.class, name = "FAILED"),
			@JsonSubTypes.Type(value = CanceledQueryResponse.class, name = "CANCELED")
	})
	@Schema(
			description = "Query response with status-based polymorphism.",
			discriminatorProperty = "status",
			oneOf = {
					NewQueryResponse.class,
					RunningQueryResponse.class,
					DoneQueryResponse.class,
					FailedQueryResponse.class,
					CanceledQueryResponse.class
			},
			discriminatorMapping = {
					@DiscriminatorMapping(value = "NEW", schema = NewQueryResponse.class),
					@DiscriminatorMapping(value = "RUNNING", schema = RunningQueryResponse.class),
					@DiscriminatorMapping(value = "DONE", schema = DoneQueryResponse.class),
					@DiscriminatorMapping(value = "FAILED", schema = FailedQueryResponse.class),
					@DiscriminatorMapping(value = "CANCELED", schema = CanceledQueryResponse.class)
			}
	)
	public abstract static class QueryResponse {
		public final String id;
		public final String label;
		public final String createdAt;
		public final boolean own;
		public final boolean shared;
		public final boolean system;
		public final List<String> tags;
		public final QueryDefinition query;
		public final String secondaryId;
		public final String ownerName;
		public final List<String> groups;
		public final boolean canExpand;
		public final List<String> availableSecondaryIds;
		public final QueryStatus status;

		protected QueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds,
				QueryStatus status
		) {
			this.id = id;
			this.label = label;
			this.createdAt = createdAt;
			this.own = own;
			this.shared = shared;
			this.system = system;
			this.tags = tags;
			this.query = query;
			this.secondaryId = secondaryId;
			this.ownerName = ownerName;
			this.groups = groups;
			this.canExpand = canExpand;
			this.availableSecondaryIds = availableSecondaryIds;
			this.status = status;
		}
	}

	public static final class NewQueryResponse extends QueryResponse {
		public NewQueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds
		) {
			super(id, label, createdAt, own, shared, system, tags, query, secondaryId, ownerName, groups, canExpand, availableSecondaryIds, QueryStatus.NEW);
		}
	}

	public static final class RunningQueryResponse extends QueryResponse {
		public final Double progress;

		public RunningQueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds,
				Double progress
		) {
			super(id, label, createdAt, own, shared, system, tags, query, secondaryId, ownerName, groups, canExpand, availableSecondaryIds, QueryStatus.RUNNING);
			this.progress = progress;
		}
	}

	public static final class DoneQueryResponse extends QueryResponse {
		public final Long numberOfResults;
		public final List<ResultUrlResponse> resultUrls;
		public final QueryType queryType;
		public final long requiredTime;
		public final boolean containsDates;

		public DoneQueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds,
				Long numberOfResults,
				List<ResultUrlResponse> resultUrls,
				QueryType queryType,
				long requiredTime,
				boolean containsDates
		) {
			super(id, label, createdAt, own, shared, system, tags, query, secondaryId, ownerName, groups, canExpand, availableSecondaryIds, QueryStatus.DONE);
			this.numberOfResults = numberOfResults;
			this.resultUrls = resultUrls;
			this.queryType = queryType;
			this.requiredTime = requiredTime;
			this.containsDates = containsDates;
		}
	}

	public static final class FailedQueryResponse extends QueryResponse {
		public final ErrorResponse error;

		public FailedQueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds,
				ErrorResponse error
		) {
			super(id, label, createdAt, own, shared, system, tags, query, secondaryId, ownerName, groups, canExpand, availableSecondaryIds, QueryStatus.FAILED);
			this.error = error;
		}
	}

	public static final class CanceledQueryResponse extends QueryResponse {
		public final ErrorResponse error;

		public CanceledQueryResponse(
				String id,
				String label,
				String createdAt,
				boolean own,
				boolean shared,
				boolean system,
				List<String> tags,
				QueryDefinition query,
				String secondaryId,
				String ownerName,
				List<String> groups,
				boolean canExpand,
				List<String> availableSecondaryIds,
				ErrorResponse error
		) {
			super(id, label, createdAt, own, shared, system, tags, query, secondaryId, ownerName, groups, canExpand, availableSecondaryIds, QueryStatus.CANCELED);
			this.error = error;
		}
	}

	public enum QueryStatus {
		NEW,
		RUNNING,
		DONE,
		FAILED,
		CANCELED
	}

	public enum QueryType {
		CONCEPT_QUERY,
		SECONDARY_ID_QUERY
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = ConceptQueryDefinition.class, name = "CONCEPT_QUERY"),
			@JsonSubTypes.Type(value = SecondaryIdQueryDefinition.class, name = "SECONDARY_ID_QUERY")
	})
	@Schema(
			description = "Submitted query definition, discriminated by `type`.",
			discriminatorProperty = "type",
			oneOf = {ConceptQueryDefinition.class, SecondaryIdQueryDefinition.class},
			discriminatorMapping = {
					@DiscriminatorMapping(value = "CONCEPT_QUERY", schema = ConceptQueryDefinition.class),
					@DiscriminatorMapping(value = "SECONDARY_ID_QUERY", schema = SecondaryIdQueryDefinition.class)
			}
	)
	public abstract static class QueryDefinition {
		public final QueryType type;
		public final QuerySubmissionPayload.QueryNode root;

		protected QueryDefinition(QueryType type, QuerySubmissionPayload.QueryNode root) {
			this.type = type;
			this.root = root;
		}
	}

	public static final class ConceptQueryDefinition extends QueryDefinition {
		public ConceptQueryDefinition(QuerySubmissionPayload.QueryNode root) {
			super(QueryType.CONCEPT_QUERY, root);
		}
	}

	public static final class SecondaryIdQueryDefinition extends QueryDefinition {
		public final String secondaryId;

		public SecondaryIdQueryDefinition(QuerySubmissionPayload.QueryNode root, String secondaryId) {
			super(QueryType.SECONDARY_ID_QUERY, root);
			this.secondaryId = secondaryId;
		}
	}

	public record ErrorResponse(
			String message,
			String code
	) {
	}

	public record ResultUrlResponse(
			String label,
			String url
	) {
	}
}
