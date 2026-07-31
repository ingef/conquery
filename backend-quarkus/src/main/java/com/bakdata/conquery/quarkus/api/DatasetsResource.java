package com.bakdata.conquery.quarkus.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.config.EntityPreviewRuntimeConfig;
import com.bakdata.conquery.quarkus.config.FormQueriesRuntimeConfig;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.services.DatasetService;
import com.bakdata.conquery.quarkus.services.EntityQueryService;
import com.bakdata.conquery.quarkus.services.QueryStateService;
import com.bakdata.conquery.quarkus.services.QueryUploadService;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;

@Path("/api/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetsResource {
	@Inject
	DatasetService datasetService;

	@Inject
	EntityPreviewRuntimeConfig entityPreviewConfig;

	@Inject
	FormQueriesRuntimeConfig formQueriesConfig;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	QueryStateService queryStateService;

	@Inject
	QueryUploadService queryUploadService;

	@Inject
	EntityQueryService entityQueryService;

	@Inject
	Instance<SecurityIdentity> identity;

	@GET
	public List<DatasetResponse> getDatasets() {
		return datasetService.listDatasets().stream()
							 .map(entry -> new DatasetResponse(entry.id().toString(), entry.label()))
							 .toList();
	}

	@GET
	@Path("/{datasetId}/entity-preview")
	public EntityPreviewResponse getEntityPreview(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);

		List<EntityPreviewResponse.LabeledSource> allSources =
				entityPreviewConfig.allSources().stream().map(source -> new EntityPreviewResponse.LabeledSource(source.name(), source.label())).toList();

		List<EntityPreviewResponse.LabeledSource> defaultSources =
				entityPreviewConfig.defaultSources().stream().map(source -> new EntityPreviewResponse.LabeledSource(source.name(), source.label())).toList();

		List<String> searchFilters = entityPreviewConfig.searchFilters()
											.map(value -> Stream.of(value.split(","))
																.map(String::trim)
																.filter(filter -> !filter.isEmpty())
																.toList())
											.orElse(List.of());
		String searchConcept = entityPreviewConfig.searchConcept().orElse(null);

		return new EntityPreviewResponse(allSources, defaultSources, searchFilters, searchConcept);
	}

	@GET
	@Path("/{datasetId}/concepts")
	@Operation(
			summary = "Get root concepts for a dataset",
			description = "Returns top-level concept nodes. Nodes with detailsAvailable=false represent folder/structure nodes."
	)
	public ConceptsResponse getConcepts(@PathParam("datasetId") String datasetId) {

		java.util.Map<String, ConceptsResponse.ConceptSummaryResponse> concepts = new LinkedHashMap<>();
		datasetService.listRootConceptsForDataset(datasetId).forEach(entry -> concepts.put(
				entry.id().toString(),
				new ConceptsResponse.ConceptSummaryResponse(
						entry.label(),
						null,
						true,
						entry.childrenIds().stream().map(ConceptId::toString).toList(),
						0L,
						0L,
						true,
						!entry.children().isEmpty(),
						// TODO Remove detailed connector filters/selects from this summary once the frontend loads them via /api/concepts/{conceptId}.
						entry.connectors().stream().map(this::toTableResponse).toList(),
						List.of()
				)
		));

		return new ConceptsResponse(
				List.of(),
				concepts
		);
	}


	private ConceptResource.ConnectorResponse toTableResponse(DatasetCatalogRepository.Connector connector) {
		// TODO combine this with ConceptResource#toConnectorResponse
		DatasetCatalogRepository.TableRecord tableRecord = datasetService.requireTable(connector.tableId());
		List<String> supportedSecondaryIds = tableRecord.columns().stream()
				.map(DatasetCatalogRepository.ColumnRecord::secondaryId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		return new ConceptResource.ConnectorResponse(
				connector.tableId().toString(),
				connector.name(),
				connector.label(),
				connector.isDefault(),
				connector.filters().stream().map(this::toFilterResponse).toList(),
				connector.selects().stream().map(this::toSelectResponse).toList(),
				supportedSecondaryIds
		);
	}

	private ConceptResource.SelectResponse toSelectResponse(DatasetCatalogRepository.Select select) {
		return new ConceptResource.SelectResponse(
				select.id().toString(),
				select.label(),
				select.description(),
				select.defaultSelected(),
				toSelectResultTypeResponse(select.resultType())
		);
	}

	private ConceptResource.SelectResultTypeResponse toSelectResultTypeResponse(DatasetCatalogRepository.SelectResultType resultType) {
		return new ConceptResource.SelectResultTypeResponse(
				resultType.type(),
				resultType.elementType() == null ? null : new ConceptResource.ElementTypeResponse(resultType.elementType().type())
		);
	}

	private ConceptResource.FilterResponse toFilterResponse(DatasetCatalogRepository.Filter filter) {
		return new ConceptResource.FilterResponse(
				filter.id().toString(),
				filter.label(),
				filter.type(),
				filter.unit(),
				filter.tooltip(),
				filter.options().stream()
						.map(option -> new ConceptResource.FrontendValue(option.value(), option.label(), option.optionValue()))
						.toList(),
				filter.min(),
				filter.max(),
				filter.pattern(),
				filter.allowDropFile(),
				filter.creatable(),
				filter.defaultValue()
				);
	}

	private ConceptResource.ColumnResponse toColumnResponse(DatasetCatalogRepository.ColumnRecord column) {
		return new ConceptResource.ColumnResponse(
				column.id().toString(),
				column.label(),
				column.type(),
				column.secondaryId()
		);
	}


	@GET
	@Path("/{datasetId}/form-queries")
	@Operation(
			summary = "Get form configurations for a dataset",
			description = "Returns raw frontend form configuration objects."
	)
	public List<Object> getFormQueries(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);
		return formQueriesConfig.resources().stream().map(this::loadFormResource).toList();
	}

	@GET
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "List queries for a dataset",
			description = "Returns the query history list for the given dataset."
	)
	public List<QuerySummaryResponse> getQueries(@PathParam("datasetId") String datasetId) {
		datasetService.requireDataset(datasetId);
		return queryStateService.getDatasetQueries(datasetId);
	}

	@POST
	@Path("/{datasetId}/queries")
	@Operation(
			summary = "Create a query for a dataset",
			description = "Accepts a query payload and returns the created query id."
	)
	public StartQueryResponse postQueries(@PathParam("datasetId") String datasetId, QuerySubmissionPayload payload) {
		datasetService.requireDataset(datasetId);
		return queryStateService.createQuery(datasetId, payload, SecurityIdentityUtil.resolveUserName(identity));
	}

	@POST
	@Path("/{datasetId}/queries/upload")
	@Operation(
			summary = "Upload query entities",
			description = "Uploads entity id rows for query upload workflow."
	)
	public UploadQueryResponse uploadQueries(
			@PathParam("datasetId") String datasetId,
			@Valid @NotNull QueryUploadPayload payload
	) {
		datasetService.requireDataset(datasetId);
		QueryUploadService.UploadResult result = queryUploadService.processUpload(
				new QueryUploadService.QueryUploadPayload(payload.format, payload.values, payload.label)
		);
		return new UploadQueryResponse(result.resolved(), result.unresolvedId(), result.unreadableDate());
	}

	@POST
	@Path("/{datasetId}/queries/entity")
	@Operation(
			summary = "Get entity history",
			description = "Returns history data for a single entity."
	)
	public EntityQueryService.EntityHistoryResponse getEntityHistory(
			@PathParam("datasetId") String datasetId,
			@Valid @NotNull EntityHistoryRequest payload
	) {
		datasetService.requireDataset(datasetId);
		return entityQueryService.getEntityHistory(
				new EntityQueryService.EntityHistoryRequest(
						payload.idKind,
						payload.entityId,
						payload.time,
						payload.sources
				)
		);
	}

	@POST
	@Path("/{datasetId}/queries/resolve-entities")
	@Operation(
			summary = "Resolve entities from filter values",
			description = "Resolves entity ids from selected filter values."
	)
	public List<Map<String, String>> resolveEntities(
			@PathParam("datasetId") String datasetId,
			@Valid @NotNull List<@Valid FilterValueRequest> payload
	) {
		datasetService.requireDataset(datasetId);
		return entityQueryService.resolveEntities(
				payload.stream()
					   .map(this::toFilterValuesRequest)
					   .toList()
		);
	}

	private EntityQueryService.FilterValuesRequest toFilterValuesRequest(FilterValueRequest request) {
		return new EntityQueryService.FilterValuesRequest(
				request.filter,
				request.type,
				extractFilterValues(request)
		);
	}

	private List<String> extractFilterValues(FilterValueRequest request) {
		return switch (request) {
			case MultiSelectFilterValueRequest multiSelect -> multiSelect.value;
			case BigMultiSelectFilterValueRequest bigMultiSelect -> bigMultiSelect.value;
			case SelectFilterValueRequest select -> List.of(select.value);
			case StringFilterValueRequest string -> List.of(string.value);
			case IntegerFilterValueRequest integer -> List.of(String.valueOf(integer.value));
			case IntegerRangeFilterValueRequest integerRange -> List.of(String.valueOf(integerRange.value));
			case MoneyRangeFilterValueRequest moneyRange -> List.of(String.valueOf(moneyRange.value));
			case RealFilterValueRequest real -> List.of(String.valueOf(real.value));
			case RealRangeFilterValueRequest realRange -> List.of(String.valueOf(realRange.value));
			default -> throw new IllegalArgumentException("Unsupported filter value type: " + request.getClass().getName());
		};
	}

	private Object loadFormResource(String path) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("Configured form resource does not exist: " + path);
			}
			return objectMapper.readValue(input, Object.class);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to parse form resource: " + path, e);
		}
	}

	public record QuerySummaryResponse(
			String id,
			String label,
			Long numberOfResults,
			String createdAt,
			List<String> tags,
			boolean own,
			String ownerName,
			boolean system,
			List<QueryResource.ResultUrlResponse> resultUrls,
			boolean shared,
			boolean canExpand,
			String queryType,
			String secondaryId,
			boolean containsDates
	) {
	}

	public record StartQueryResponse(
			String id
	) {
	}

	public record UploadQueryResponse(
			int resolved,
			List<List<String>> unresolvedId,
			List<List<String>> unreadableDate
	) {
	}

	public static final class QueryUploadPayload {
		public final @NotNull @NotEmpty List<@NotBlank String> format;
		public final @NotNull List<@NotNull List<@NotBlank String>> values;
		public final @NotBlank String label;

		public QueryUploadPayload(List<String> format, List<List<String>> values, String label) {
			this.format = format;
			this.values = values;
			this.label = label;
		}
	}

	public static final class EntityHistoryRequest {
		public final @NotBlank String idKind;
		public final @NotBlank String entityId;
		public final @NotNull @Valid QuerySubmissionPayload.DateRangePayload time;
		public final @NotNull @NotEmpty List<@NotBlank String> sources;

		public EntityHistoryRequest(String idKind, String entityId, QuerySubmissionPayload.DateRangePayload time, List<String> sources) {
			this.idKind = idKind;
			this.entityId = entityId;
			this.time = time;
			this.sources = sources;
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = MultiSelectFilterValueRequest.class, name = "MULTI_SELECT"),
			@JsonSubTypes.Type(value = BigMultiSelectFilterValueRequest.class, name = "BIG_MULTI_SELECT"),
			@JsonSubTypes.Type(value = SelectFilterValueRequest.class, name = "SELECT"),
			@JsonSubTypes.Type(value = StringFilterValueRequest.class, name = "STRING"),
			@JsonSubTypes.Type(value = IntegerFilterValueRequest.class, name = "INTEGER"),
			@JsonSubTypes.Type(value = IntegerRangeFilterValueRequest.class, name = "INTEGER_RANGE"),
			@JsonSubTypes.Type(value = MoneyRangeFilterValueRequest.class, name = "MONEY_RANGE"),
			@JsonSubTypes.Type(value = RealFilterValueRequest.class, name = "REAL"),
			@JsonSubTypes.Type(value = RealRangeFilterValueRequest.class, name = "REAL_RANGE")
	})
	@org.eclipse.microprofile.openapi.annotations.media.Schema(
			description = "Filter value payload discriminated by `type`.",
			discriminatorProperty = "type",
			oneOf = {
					MultiSelectFilterValueRequest.class,
					BigMultiSelectFilterValueRequest.class,
					SelectFilterValueRequest.class,
					StringFilterValueRequest.class,
					IntegerFilterValueRequest.class,
					IntegerRangeFilterValueRequest.class,
					MoneyRangeFilterValueRequest.class,
					RealFilterValueRequest.class,
					RealRangeFilterValueRequest.class
			},
			discriminatorMapping = {
					@DiscriminatorMapping(value = "MULTI_SELECT", schema = MultiSelectFilterValueRequest.class),
					@DiscriminatorMapping(value = "BIG_MULTI_SELECT", schema = BigMultiSelectFilterValueRequest.class),
					@DiscriminatorMapping(value = "SELECT", schema = SelectFilterValueRequest.class),
					@DiscriminatorMapping(value = "STRING", schema = StringFilterValueRequest.class),
					@DiscriminatorMapping(value = "INTEGER", schema = IntegerFilterValueRequest.class),
					@DiscriminatorMapping(value = "INTEGER_RANGE", schema = IntegerRangeFilterValueRequest.class),
					@DiscriminatorMapping(value = "MONEY_RANGE", schema = MoneyRangeFilterValueRequest.class),
					@DiscriminatorMapping(value = "REAL", schema = RealFilterValueRequest.class),
					@DiscriminatorMapping(value = "REAL_RANGE", schema = RealRangeFilterValueRequest.class)
			}
	)
	public abstract static class FilterValueRequest {
		public final @NotBlank String filter;
		public final @NotBlank String type;

		protected FilterValueRequest(String filter, String type) {
			this.filter = filter;
			this.type = type;
		}
	}

	public static final class MultiSelectFilterValueRequest extends FilterValueRequest {
		public final @NotNull @NotEmpty List<@NotBlank String> value;

		public MultiSelectFilterValueRequest(String filter, List<String> value) {
			super(filter, "MULTI_SELECT");
			this.value = value;
		}
	}

	public static final class BigMultiSelectFilterValueRequest extends FilterValueRequest {
		public final @NotNull @NotEmpty List<@NotBlank String> value;

		public BigMultiSelectFilterValueRequest(String filter, List<String> value) {
			super(filter, "BIG_MULTI_SELECT");
			this.value = value;
		}
	}

	public static final class SelectFilterValueRequest extends FilterValueRequest {
		public final @NotBlank String value;

		public SelectFilterValueRequest(String filter, String value) {
			super(filter, "SELECT");
			this.value = value;
		}
	}

	public static final class StringFilterValueRequest extends FilterValueRequest {
		public final @NotBlank String value;

		public StringFilterValueRequest(String filter, String value) {
			super(filter, "STRING");
			this.value = value;
		}
	}

	public static final class IntegerFilterValueRequest extends FilterValueRequest {
		public final @NotNull Long value;

		public IntegerFilterValueRequest(String filter, Long value) {
			super(filter, "INTEGER");
			this.value = value;
		}
	}

	public static final class IntegerRangeFilterValueRequest extends FilterValueRequest {
		public final @NotNull NumericRangeValue value;

		public IntegerRangeFilterValueRequest(String filter, NumericRangeValue value) {
			super(filter, "INTEGER_RANGE");
			this.value = value;
		}
	}

	public static final class MoneyRangeFilterValueRequest extends FilterValueRequest {
		public final @NotNull NumericRangeValue value;

		public MoneyRangeFilterValueRequest(String filter, NumericRangeValue value) {
			super(filter, "MONEY_RANGE");
			this.value = value;
		}
	}

	public static final class RealFilterValueRequest extends FilterValueRequest {
		public final @NotNull Double value;

		public RealFilterValueRequest(String filter, Double value) {
			super(filter, "REAL");
			this.value = value;
		}
	}

	public static final class RealRangeFilterValueRequest extends FilterValueRequest {
		public final @NotNull DecimalRangeValue value;

		public RealRangeFilterValueRequest(String filter, DecimalRangeValue value) {
			super(filter, "REAL_RANGE");
			this.value = value;
		}
	}

	public static final class NumericRangeValue {
		public Long min;
		public Long max;
	}

	public static final class DecimalRangeValue {
		public Double min;
		public Double max;
	}

	public record DatasetResponse(
			String id,
			String label
	) {
	}
}
