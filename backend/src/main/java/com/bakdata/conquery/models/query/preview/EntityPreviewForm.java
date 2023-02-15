package com.bakdata.conquery.models.query.preview;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
public class EntityPreviewForm extends Form implements InternalForm {

	public static final String INFOS_QUERY_NAME = "INFOS";
	public static final String VALUES_QUERY_NAME = "VALUES";

	private final AbsoluteFormQuery infoCardQuery;
	private final TableExportQuery valuesQuery;


	@Nullable
	@Override
	public JsonNode getValues() {
		return null; // will not be implemented.
	}

	public static EntityPreviewForm create(String entity, String idKind, Range<LocalDate> dateRange, List<Connector> sources, List<Select> infos) {

		// We use this query to filter for the single selected query.
		final Query entitySelectQuery = new ConceptQuery(new CQExternal(List.of(idKind), new String[][]{{"HEAD"}, {entity}}, true));

		// Query exporting selected Sources of the Entity.
		final TableExportQuery exportQuery = new TableExportQuery(entitySelectQuery);

		exportQuery.setDateRange(dateRange);
		exportQuery.setTables(sources.stream().map(CQConcept::forConnector).collect(Collectors.toList()));
		exportQuery.setRawConceptValues(false);

		// Query exporting a few additional infos on the entity.
		final AbsoluteFormQuery infoCardQuery =
				new AbsoluteFormQuery(entitySelectQuery, dateRange,
									  ArrayConceptQuery.createFromFeatures(
											  infos.stream()
												   .map(CQConcept::forSelect)
												   .collect(Collectors.toList())
									  ),
									  List.of(ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN))
				);

		return new EntityPreviewForm(infoCardQuery, exportQuery);
	}


	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(Namespace namespace, User user, Dataset submittedDataset, MetaStorage storage) {
		return Map.of(
				VALUES_QUERY_NAME, List.of(getValuesQuery().toManagedExecution(user, submittedDataset, storage)),
				INFOS_QUERY_NAME, List.of(getInfoCardQuery().toManagedExecution(user, submittedDataset, storage))
		);
	}

	@Override
	public void authorize(Subject subject, Dataset submittedDataset, @NonNull ClassToInstanceMap<QueryVisitor> visitors, MetaStorage storage) {
		QueryDescription.authorizeQuery(this, subject, submittedDataset, visitors, storage);
	}

	@Override
	public String getLocalizedTypeLabel() {
		// If we successfully keep away system queries from the users, this should not be called except for buildStatusFull, where it is ignored.
		return getClass().getAnnotation(CPSType.class).id();
	}

	@Override
	public ManagedExecution toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new EntityPreviewExecution(this, user, submittedDataset, storage);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Sets.union(getValuesQuery().collectRequiredQueries(), getInfoCardQuery().collectRequiredQueries());
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		getInfoCardQuery().visit(visitor);
		getValuesQuery().visit(visitor);

		visitor.accept(this);
	}
}
