package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "FULL_EXPORT_FORM", base = QueryDescription.class)
public class FullExportForm extends Form implements InternalForm {

	@NotNull
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@Nullable
	@Valid
	private Range<LocalDate> dateRange = Range.all();

	@NotEmpty
	private List<CQConcept> tables = ImmutableList.of();

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		tables.forEach(feature -> feature.visit(visitor));
	}


	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(Namespace namespace, User user, MetaStorage storage) {

		// Forms are sent as an array of standard queries containing AND/OR of CQConcepts, we ignore everything and just convert the CQConcepts into CQUnfiltered for export.

		final TableExportQuery exportQuery = new TableExportQuery(queryGroup.getQuery());
		exportQuery.setDateRange(getDateRange());

		exportQuery.setTables(tables);

		final ManagedQuery managedQuery = new ManagedQuery(exportQuery, user, namespace.getDataset(), storage);


		return Map.of(
				ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
				List.of(managedQuery)
		);

	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getStorage().getExecution(queryGroupId);
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(ExportFormC10n.class, I18n.LOCALE.get()).getType();
	}


	@Override
	public ManagedInternalForm<FullExportForm> toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new ManagedInternalForm<FullExportForm>(this, user, submittedDataset, storage);
	}
}
