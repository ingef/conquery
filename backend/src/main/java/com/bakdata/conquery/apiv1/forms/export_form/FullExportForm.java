package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.InternalForm;
import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@CPSType(id = "FULL_EXPORT_FORM", base = QueryDescription.class)
public class FullExportForm extends Form implements InternalForm {

	@Nullable
	@Getter
	@Setter
	@EqualsAndHashCode.Exclude
	private JsonNode values;


	@Nullable
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
	public Map<String, Query> createSubQueries() {

		// Forms are sent as an array of standard queries containing AND/OR of CQConcepts, we ignore everything and just convert the CQConcepts into CQUnfiltered for export.

		final Query query;

		if (queryGroupId != null) {
			query = queryGroup.getQuery();
		}
		else {
			query = new ConceptQuery(new CQYes());
		}

		final TableExportQuery exportQuery = new TableExportQuery(query);
		exportQuery.setDateRange(getDateRange());

		exportQuery.setTables(tables);


		return Map.of(
				ConqueryConstants.SINGLE_RESULT_TABLE_NAME,
				exportQuery
		);

	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		if (queryGroupId == null) {
			return Collections.emptySet();
		}

		return Set.of(queryGroupId);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		if (queryGroupId != null) {
			queryGroup = (ManagedQuery) context.getStorage().getExecution(queryGroupId);
		}
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
