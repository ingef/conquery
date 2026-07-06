package com.bakdata.conquery.quarkus.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.api.QuerySubmissionPayload;
import com.bakdata.conquery.quarkus.config.FrontendRuntimeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EntityQueryService {

	@Inject
	FrontendRuntimeConfig frontendConfig;

	public EntityHistoryResponse getEntityHistory(EntityHistoryRequest request) {
		return new EntityHistoryResponse(
				List.of(),
				List.of(),
				List.of(
						new EntityInfoResponse(
								"entityId",
								request.entityId(),
								"STRING",
								List.of()
						)
				),
				List.of()
		);
	}

	public List<Map<String, String>> resolveEntities(List<FilterValuesRequest> filters) {
		String idKind = resolveOutputIdKind();
		return filters.stream()
					  .flatMap(filter -> filter.value().stream().map(value -> Map.of(idKind, syntheticEntityId(filter.filter(), value))))
					  .distinct()
					  .collect(Collectors.toCollection(ArrayList::new));
	}

	private String resolveOutputIdKind() {
		Optional<List<FrontendRuntimeConfig.IdColumn>> configured = frontendConfig.queryUpload().ids();
		if (configured.isPresent() && !configured.get().isEmpty()) {
			return configured.get().stream()
							 .filter(FrontendRuntimeConfig.IdColumn::print)
							 .findFirst()
							 .map(FrontendRuntimeConfig.IdColumn::name)
							 .orElse(configured.get().getFirst().name());
		}
		return "ID";
	}

	private String syntheticEntityId(String filterId, String filterValue) {
		// TODO(quarkus-migration): Replace synthetic IDs with actual entity resolution against dataset data.
		//  The legacy backend executes a concept query and returns matching entity IDs.
		String input = (filterId == null ? "" : filterId) + "|" + (filterValue == null ? "" : filterValue);
		return "entity-" + Integer.toUnsignedString(input.hashCode(), 36);
	}

	public record EntityHistoryRequest(
			String idKind,
			String entityId,
			QuerySubmissionPayload.DateRangePayload time,
			List<String> sources
	) {
	}

	public record FilterValuesRequest(
			String filter,
			String type,
			List<String> value
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
