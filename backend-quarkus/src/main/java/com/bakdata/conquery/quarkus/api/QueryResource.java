package com.bakdata.conquery.quarkus.api;

import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/queries")
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {

	@GET
	@Path("/{queryId}")
	@Operation(
			summary = "Get a query by id",
			description = "Returns the current query execution status and metadata."
	)
	public QueryResponse getQuery(@PathParam("queryId") String queryId) {
		return new QueryResponse(
				queryId,
				"Query " + queryId,
				Instant.now().toString(),
				true,
				false,
				false,
				List.of(),
				null,
				null,
				"anonymous",
				List.of(),
				false,
				List.of(),
				"NEW",
				null,
				null,
				null,
				null,
				"CONCEPT_QUERY",
				0L,
				false
		);
	}

	public record QueryResponse(
			String id,
			String label,
			String createdAt,
			boolean own,
			boolean shared,
			boolean system,
			List<String> tags,
			Object query,
			String secondaryId,
			String ownerName,
			List<String> groups,
			boolean canExpand,
			List<String> availableSecondaryIds,
			String status,
			Double progress,
			String error,
			Long numberOfResults,
			List<ResultUrlResponse> resultUrls,
			String queryType,
			long requiredTime,
			boolean containsDates
	) {
	}

	public record ResultUrlResponse(
			String label,
			String url
	) {
	}
}
