package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.internationalization.ExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.TableExportQuery;
import com.bakdata.conquery.models.query.concept.filter.CQUnfilteredTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "FULL_EXPORT_FORM", base = QueryDescription.class)
public class FullExportForm extends Form {

	@NotNull
	@JsonProperty("queryGroup")
	private ManagedExecutionId queryGroupId;

	@JsonIgnore
	private ManagedQuery queryGroup;

	@NotNull
	@Valid
	private Range<LocalDate> dateRange;

	@NotEmpty
	private List<CQElement> features = ImmutableList.of();

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		features.forEach(feature -> feature.visit(visitor));
	}


	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {

		final List<CQUnfilteredTable> unfilteredTables =
				features.stream()
						.flatMap(feature -> {
							final Stream.Builder<Visitable> builder = Stream.builder();
							feature.visit(builder);
							return builder.build();
						})
						.filter(CQConcept.class::isInstance)
						.map(CQConcept.class::cast)
						.flatMap(concept -> concept.getTables().stream())
						.map(table -> {
							final CQUnfilteredTable unfilteredTable = new CQUnfilteredTable();

							unfilteredTable.setTable(table.getConnector());
							unfilteredTable.setDateColumn(table.getDateColumn());

							return unfilteredTable;
						})
						.collect(Collectors.toList());

		final TableExportQuery exportQuery = new TableExportQuery(queryGroup.getQuery());
		exportQuery.setDateRange(getDateRange());

		exportQuery.setTables(unfilteredTables);

		final ManagedQuery managedQuery = new ManagedQuery(exportQuery, user, submittedDataset);


		return Map.of(
				ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
				List.of(managedQuery)
		);

	}

	@Override
	public Set<ManagedExecution> collectRequiredQueries() {
		if (queryGroup == null) {
			return Collections.emptySet();
		}

		return Set.of(queryGroup);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup = (ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryGroupId);
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(ExportFormC10n.class, I18n.LOCALE.get()).getType();
	}


	@Override
	public ManagedForm toManagedExecution(User user, Dataset submittedDataset) {
		return new ManagedInternalForm(this, user, submittedDataset);
	}
}
