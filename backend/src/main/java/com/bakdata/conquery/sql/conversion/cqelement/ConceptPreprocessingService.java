package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class ConceptPreprocessingService {

	private static final String DATE_RESTRICTION_COLUMN_NAME = "date_restriction";
	private static final String VALIDITY_DATE_COLUMN_NAME_SUFFIX = "_validity_date";
	private final CQConcept concept;
	private final ConversionContext context;
	private final SqlFunctionProvider sqlFunctionProvider;

	public ConceptPreprocessingService(CQConcept concept, ConversionContext context) {
		this.concept = concept;
		this.context = context;
		this.sqlFunctionProvider = this.context.getSqlDialect().getFunction();
	}

	/**
	 * selects:
	 * - (primary column)
	 * - date restriction
	 * - validity date
	 * - any filter (group/event)
	 * - any select (group/event)
	 */
	public QueryStep buildPreprocessingQueryStepForTable(String conceptLabel, CQTable table) {

		ConceptSelects.ConceptSelectsBuilder selectsBuilder = ConceptSelects.builder();

		selectsBuilder.primaryColumn(DSL.field(context.getConfig().getPrimaryColumn()));
		selectsBuilder.dateRestriction(this.getDateRestrictionSelect(table));
		selectsBuilder.validityDate(this.getValidityDateSelect(table, conceptLabel));

		List<Field<Object>> conceptSelectFields = this.getColumnSelectReferences(table);
		List<Field<Object>> conceptFilterFields = this.getColumnFilterReferences(table);

		// deduplicate because a concepts selects and filters can require the same columns
		// and selecting the same columns several times will cause SQL errors
		List<Field<Object>> deduplicatedFilterFields = conceptFilterFields.stream()
																		  .filter(field -> !conceptSelectFields.contains(field))
																		  .toList();

		selectsBuilder.eventSelect(conceptSelectFields);
		selectsBuilder.eventFilter(deduplicatedFilterFields);

		// not part of preprocessing yet
		selectsBuilder.groupSelect(Collections.emptyList())
					  .groupFilter(Collections.emptyList());

		return QueryStep.builder()
						.cteName(this.getPreprocessingStepLabel(conceptLabel))
						.fromTable(QueryStep.toTableLike(this.getFromTableName(table)))
						.selects(selectsBuilder.build())
						.conditions(Collections.emptyList())
						.predecessors(Collections.emptyList())
						.build();
	}

	private Optional<Field<Object>> getDateRestrictionSelect(CQTable table) {
		if (!this.context.dateRestrictionActive() || !this.tableHasValidityDates(table)) {
			return Optional.empty();
		}
		CDateRange dateRestrictionRange = this.context.getDateRestrictionRange();
		Field<Object> dateRestriction = this.sqlFunctionProvider.daterange(dateRestrictionRange)
																.as(DATE_RESTRICTION_COLUMN_NAME);
		return Optional.of(dateRestriction);
	}

	private Optional<Field<Object>> getValidityDateSelect(CQTable table, String conceptLabel) {
		if (!this.validityDateIsRequired(table)) {
			return Optional.empty();
		}
		Field<Object> validityDateRange = this.sqlFunctionProvider.daterange(table.findValidityDateColumn())
																  .as(conceptLabel + VALIDITY_DATE_COLUMN_NAME_SUFFIX);
		return Optional.of(validityDateRange);
	}

	/**
	 * @return True, if a date restriction is active and the node is not excluded from time aggregation
	 * OR there is no date restriction, but still existing validity dates which are included in time aggregation.
	 */
	private boolean validityDateIsRequired(CQTable table) {
		return this.tableHasValidityDates(table)
			   && !this.concept.isExcludeFromTimeAggregation();
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

	private String getFromTableName(CQTable table) {
		return table.getConnector()
					.getTable()
					.getName();
	}

	private Field<Object> mapColumnOntoTable(Column column, CQTable table) {
		return DSL.field(DSL.name(this.getFromTableName(table), column.getName()));
	}

	private String getPreprocessingStepLabel(String conceptLabel) {
		return "concept_%s_preprocessing".formatted(conceptLabel);
	}


}
