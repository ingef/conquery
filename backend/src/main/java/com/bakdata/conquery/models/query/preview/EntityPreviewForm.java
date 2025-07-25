package com.bakdata.conquery.models.query.preview;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @implNote This Form should only be used by invocations of {@link com.bakdata.conquery.apiv1.QueryProcessor#getSingleEntityExport}.
 * <p>
 * It is used to create an overview of data for a specific entity. This is done using {@link TableExportQuery} which is quite tightly coupled to this form, BUT may be used for other use-cases.
 * <p>
 * It will also submit a query collecting identifying information of the entity to be displayed separately (e.g.: name, age, costs-sum)
 * <p>
 * The results of both valuesQuery and infoCardQuery are crammed into a single {@link EntityPreviewStatus} via a dedicated {@link EntityPreviewExecution}:
 * 1) The result of valuesQuery will act as the normal {@link com.bakdata.conquery.models.query.SingleTableResult}
 * 2) While infoCardQuery will be transformed and stored in {@link EntityPreviewStatus#getInfos()}.
 */
@CPSType(id = "ENTITY_PREVIEW", base = QueryDescription.class)
@Getter
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@ToString
public class EntityPreviewForm extends Form implements InternalForm {

	public static final String INFOS_QUERY_NAME = "INFOS";
	public static final String VALUES_QUERY_NAME = "VALUES";


	@Valid
	private final AbsoluteFormQuery infoCardQuery;
	@Valid
	private final TableExportQuery valuesQuery;


	private final Map<String, AbsoluteFormQuery> timeOverViews;

	public static EntityPreviewForm create(
			String entity,
			String idKind,
			Range<LocalDate> dateRange,
			List<ConnectorId> sources,
			List<SelectId> infos,
			List<PreviewConfig.TimeStratifiedSelects> timeStratifiedSelects,
			DatasetRegistry<?> datasetRegistry) {

		// We use this query to filter for the single selected query.
		final Query entitySelectQuery = new ConceptQuery(new CQExternal(List.of(idKind), new String[][]{{"HEAD"}, {entity}}, true));

		final TableExportQuery exportQuery = createExportQuery(dateRange, sources, entitySelectQuery);

		final AbsoluteFormQuery infoCardQuery = createInfoCardQuery(dateRange, infos, entitySelectQuery);

		final Map<String, AbsoluteFormQuery> timeQueries = createTimeStratifiedQueries(dateRange, timeStratifiedSelects, datasetRegistry, entitySelectQuery);

		return new EntityPreviewForm(infoCardQuery, exportQuery, timeQueries);
	}

	@NotNull
	private static TableExportQuery createExportQuery(Range<LocalDate> dateRange, List<ConnectorId> sources, Query entitySelectQuery) {
		// Query exporting selected Sources of the Entity.
		final TableExportQuery exportQuery = new TableExportQuery(entitySelectQuery);

		exportQuery.setDateRange(dateRange);
		exportQuery.setConcepts(sources.stream().map(CQConcept::forConnector).collect(Collectors.toList()));
		exportQuery.setRawConceptValues(false);
		return exportQuery;
	}

	@NotNull
	private static AbsoluteFormQuery createInfoCardQuery(Range<LocalDate> dateRange, List<SelectId> infos, Query entitySelectQuery) {
		// Query exporting a few additional infos on the entity.
		return new AbsoluteFormQuery(entitySelectQuery, dateRange,
							 ArrayConceptQuery.createFromFeatures(
									  infos.stream()
										   .map(CQConcept::forSelect)
										   .collect(Collectors.toList())
							  ),
							 List.of(ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN))
		);
	}

	@NotNull
	private static Map<String, AbsoluteFormQuery> createTimeStratifiedQueries(Range<LocalDate> dateRange, List<PreviewConfig.TimeStratifiedSelects> timeStratifiedSelects, DatasetRegistry<?> datasetRegistry, Query entitySelectQuery) {
		final Map<String, AbsoluteFormQuery> timeQueries = new HashMap<>();

		// per group create an AbsoluteFormQuery on years and quarters.
		for (PreviewConfig.TimeStratifiedSelects selects : timeStratifiedSelects) {

			final AbsoluteFormQuery query = new AbsoluteFormQuery(entitySelectQuery, dateRange,
																  ArrayConceptQuery.createFromFeatures(
																		  selects.selects().stream()
																				 .map(PreviewConfig.InfoCardSelect::select)
																				 .map(CQConcept::forSelect)
																				 .collect(Collectors.toList())),
																  List.of(
																		  ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN),
																		  ExportForm.ResolutionAndAlignment.of(Resolution.YEARS, Alignment.YEAR),
																		  ExportForm.ResolutionAndAlignment.of(Resolution.QUARTERS, Alignment.QUARTER)
																  )
			);

			timeQueries.put(selects.label(), query);
		}
		return timeQueries;
	}

	@Nullable
	@Override
	@JsonIgnore
	public JsonNode getValues() {
		return null; // will not be implemented.
	}

	@Override
	public void authorize(Subject subject, DatasetId submittedDataset, @NonNull List<QueryVisitor> visitors, MetaStorage storage) {
		QueryDescription.authorizeQuery(this, subject, submittedDataset, visitors, storage);
	}

	@Override
	public String getLocalizedTypeLabel() {
		// If we successfully keep away system queries from the users, this should not be called except for buildStatusFull, where it is ignored.
		return getClass().getAnnotation(CPSType.class).id();
	}

	@Override
	public Map<String, Query> createSubQueries() {

		final Map<String, Query> subQueries = new HashMap<>();
		subQueries.put(VALUES_QUERY_NAME, getValuesQuery());
		subQueries.put(INFOS_QUERY_NAME, getInfoCardQuery());

		subQueries.putAll(getTimeOverViews());

		return subQueries;
	}

	@Override
	public ManagedExecution toManagedExecution(UserId user, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		return new EntityPreviewExecution(this, user, submittedDataset, storage, datasetRegistry, config);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		final Set<ManagedExecutionId> required = new HashSet<>();

		for (AbsoluteFormQuery query : getTimeOverViews().values()) {
			query.collectRequiredQueries(required);
		}

		getValuesQuery().collectRequiredQueries(required);

		getInfoCardQuery().collectRequiredQueries(required);

		return required;
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		getInfoCardQuery().visit(visitor);
		getValuesQuery().visit(visitor);

		getTimeOverViews().values().forEach(v -> v.visit(visitor));

		visitor.accept(this);
	}
}
