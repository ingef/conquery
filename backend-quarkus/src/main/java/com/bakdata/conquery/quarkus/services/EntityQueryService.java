package com.bakdata.conquery.quarkus.services;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.api.QuerySubmissionPayload;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class EntityQueryService {

	// TODO(quarkus-migration): Implement entity queries against dataset data. The records below only preserve the API models.
	public EntityHistoryResponse getEntityHistory(EntityHistoryRequest request) {
		throw notImplemented();
	}

	public List<Map<String, String>> resolveEntities(List<FilterValue> filters) {
		throw notImplemented();
	}

	private WebApplicationException notImplemented() {
		return new WebApplicationException("Entity queries are not implemented yet.", Response.Status.NOT_IMPLEMENTED);
	}

	public record EntityHistoryRequest(
			String idKind,
			String entityId,
			QuerySubmissionPayload.DateRangePayload time,
			List<String> sources
	) {
	}

	public record EntityHistoryResponse(
			List<ResultUrlWithLabelResponse> resultUrls,
			List<ColumnDescriptionResponse> columnDescriptions,
			List<EntityInfoResponse> infos,
			List<TimeStratifiedInfoResponse> timeStratifiedInfos
	) {
	}

	public record ResultUrlWithLabelResponse(
			String label,
			String url
	) {
	}

	public record ColumnDescriptionResponse(
			String label,
			String description,
			String type,
			List<Map<String, String>> semantics,
			String defaultLabel,
			String selectId,
			String userConceptLabel
	) {
	}

	public record EntityInfoResponse(
			String label,
			String value,
			String type,
			List<Map<String, String>> semantics
	) {
	}

	public record TimeStratifiedInfoResponse(
			String label,
			String description,
			Map<String, Object> totals,
			List<ColumnDescriptionResponse> columns,
			List<TimeStratifiedInfoYearResponse> years
	) {
	}

	public record TimeStratifiedInfoYearResponse(
			int year,
			Map<String, Object> values,
			List<TimeStratifiedInfoQuarterResponse> quarters
	) {
	}

	public record TimeStratifiedInfoQuarterResponse(
			int quarter,
			Map<String, String> values
	) {
	}
}
