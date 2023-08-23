package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import org.jooq.Field;
import org.jooq.impl.DSL;

class PreprocessingQueryStep extends ConceptQueryStep {

	public boolean canConvert(StepContext stepContext) {
		// We always apply preprocessing to select the required columns
		return true;
	}

	public QueryStep.QueryStepBuilder convertStep(StepContext stepContext) {

		CQTable table = stepContext.getTable();
		ConceptSelects.ConceptSelectsBuilder selectsBuilder = ConceptSelects.builder();

		selectsBuilder.primaryColumn(DSL.field(DSL.name(stepContext.getContext().getConfig().getPrimaryColumn())))
					  .dateRestrictionRange(this.getDateRestrictionSelect(stepContext))
					  .validityDate(this.getValidityDateSelect(stepContext));

		List<Field<Object>> conceptSelectFields = this.getColumnSelectReferences(table);
		List<Field<Object>> conceptFilterFields = this.getColumnFilterReferences(table);

		// deduplicate because a concepts selects and filters can require the same columns
		// and selecting the same columns several times will cause SQL errors
		List<Field<Object>> deduplicatedFilterFields = conceptFilterFields.stream()
																		  .filter(field -> !conceptSelectFields.contains(field))
																		  .toList();

		selectsBuilder.eventSelect(conceptSelectFields).
					  eventFilter(deduplicatedFilterFields);

		// not part of preprocessing yet
		selectsBuilder.groupSelect(Collections.emptyList())
					  .groupFilter(Collections.emptyList());

		return QueryStep.builder()
						.selects(selectsBuilder.build())
						.conditions(Collections.emptyList())
						.predecessors(Collections.emptyList());
	}

	@Override
	public String nameSuffix() {
		return "_preprocessing";
	}

	private Optional<ColumnDateRange> getDateRestrictionSelect(final StepContext stepContext) {
		if (!stepContext.getContext().dateRestrictionActive() || !this.tableHasValidityDates(stepContext.getTable())) {
			return Optional.empty();
		}
		ColumnDateRange dateRestriction = stepContext.getContext().getSqlDialect().getFunction().daterange(stepContext.getContext().getDateRestrictionRange());
		return Optional.of(dateRestriction);
	}

	private Optional<ColumnDateRange> getValidityDateSelect(final StepContext stepContext) {
		if (!this.validityDateIsRequired(stepContext)) {
			return Optional.empty();
		}
		return Optional.of(stepContext.getSqlFunctions().daterange(stepContext.getTable().findValidityDate(), stepContext.getConceptLabel()));
	}

	/**
	 * @return True, if a date restriction is active and the node is not excluded from time aggregation
	 * OR there is no date restriction, but still existing validity dates which are included in time aggregation.
	 */
	private boolean validityDateIsRequired(final StepContext stepContext) {
		return this.tableHasValidityDates(stepContext.getTable())
			   && !stepContext.getNode().isExcludeFromTimeAggregation();
	}

	private boolean tableHasValidityDates(CQTable table) {
		return !table.getConnector()
					 .getValidityDates()
					 .isEmpty();
	}

	private List<Field<Object>> getColumnSelectReferences(CQTable table) {
		return table.getSelects().stream()
					.flatMap(select -> select.getRequiredColumns().stream().map(column -> this.mapColumnOntoTable(column, table)))
					.toList();
	}

	private List<Field<Object>> getColumnFilterReferences(CQTable table) {
		return table.getFilters().stream()
					.map(FilterValue::getFilter)
					.flatMap(filter -> filter.getRequiredColumns().stream().map(column -> this.mapColumnOntoTable(column, table)))
					.toList();
	}


	private Field<Object> mapColumnOntoTable(Column column, CQTable table) {
		return DSL.field(DSL.name(table.getConnector().getTable().getName(), column.getName()));}


}
