package com.bakdata.conquery.models.query.preview;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
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
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ClassToInstanceMap;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "ENTITY_PREVIEW", base = QueryDescription.class)
@Data
public class EntityPreviewForm extends Form {

	public static final String INFOS_QUERY_NAME = "INFOS";
	public static final String VALUES_QUERY_NAME = "VALUES";

	private final String entity;
	private final String idKind;
	private final Range<LocalDate> dateRange;
	@NsIdRefCollection
	private final List<Connector> sources;
	@NsIdRefCollection
	private final List<Select> infos;

	@Nullable
	@Override
	public JsonNode getValues() {
		return null; //TODO will not be implemented.
	}

	public static EntityPreviewForm create(String entity, String idKind, Range<LocalDate> dateRange, List<Connector> sources, List<Select> infos) {
		return new EntityPreviewForm(entity, idKind, dateRange, sources, infos);
	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		final Query entitySelectQuery = new ConceptQuery(new CQExternal(List.of(idKind), new String[][]{{"HEAD"}, {entity}}, true));

		final TableExportQuery exportQuery = new TableExportQuery(entitySelectQuery);
		exportQuery.setDateRange(dateRange);

		exportQuery.setTables(sources.stream().map(CQConcept::forConnector).collect(Collectors.toList()));

		final Query infoCardQuery =
				new AbsoluteFormQuery(entitySelectQuery, dateRange,
									  ArrayConceptQuery.createFromFeatures(
											  infos.stream()
												   .map(CQConcept::forSelect)
												   .collect(Collectors.toList())),
									  List.of(ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN))
				);

		return Map.of(
				VALUES_QUERY_NAME, List.of(exportQuery.toManagedExecution(user, submittedDataset)),
				INFOS_QUERY_NAME, List.of(infoCardQuery.toManagedExecution(user, submittedDataset))
		);
	}

	@Override
	public void authorize(Subject subject, Dataset submittedDataset, @NonNull ClassToInstanceMap<QueryVisitor> visitors, MetaStorage storage) {
		QueryDescription.authorizeQuery(this, subject, submittedDataset, visitors, storage);

	}

	@Override
	public String getLocalizedTypeLabel() {
		return "null"; //TODO what do?
	}

	@Override
	public ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset) {
		return new EntityPreviewExecution(this, user, submittedDataset);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Collections.emptySet();
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
