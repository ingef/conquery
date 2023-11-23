package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class PostgreSqlDateAggregator implements SqlDateAggregator {

	private final static String DATE_AGGREGATION_CTE_NAME = "_dates_aggregated";
	private final static String INVERTED_DATE_AGGREGATION_CTE_NAME = "_dates_inverted";

	private final SqlFunctionProvider functionProvider;

	public PostgreSqlDateAggregator(SqlFunctionProvider functionProvider) {
		this.functionProvider = functionProvider;
	}

	@Override
	public QueryStep apply(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction
	) {
		String joinedStepCteName = joinedStep.getCteName();

		ColumnDateRange aggregatedValidityDate = getAggregatedValidityDate(dateAggregationDates, dateAggregationAction, joinedStepCteName);

		Selects dateAggregationSelects = Selects.builder()
												.primaryColumn(joinedStep.getQualifiedSelects().getPrimaryColumn())
												.validityDate(Optional.ofNullable(aggregatedValidityDate))
												.sqlSelects(QualifyingUtil.qualify(carryThroughSelects, joinedStepCteName))
												.build();

		return QueryStep.builder()
						.cteName(joinedStepCteName + DATE_AGGREGATION_CTE_NAME)
						.selects(dateAggregationSelects)
						.fromTable(QueryStep.toTableLike(joinedStepCteName))
						.predecessors(List.of(joinedStep))
						.build();
	}

	@Override
	public QueryStep invertAggregatedIntervals(QueryStep baseStep) {

		Selects baseStepSelects = baseStep.getQualifiedSelects();
		Optional<ColumnDateRange> validityDate = baseStepSelects.getValidityDate();
		if (validityDate.isEmpty()) {
			return baseStep;
		}

		Field<Object> maxDateRange = DSL.function(
				"daterange",
				Object.class,
				this.functionProvider.toDateField(this.functionProvider.getMinDateExpression()),
				this.functionProvider.toDateField(this.functionProvider.getMaxDateExpression()),
				DSL.val("[]")
		);

		// see https://www.postgresql.org/docs/current/functions-range.html
		// {[-infinity,infinity]} - {multirange} computes the inverse of a {multirange}
		Field<Object> invertedValidityDate = DSL.field(
				"{0}::datemultirange - {1}",
				Object.class,
				maxDateRange,
				validityDate.get().getRange()
		).as(DATE_AGGREGATION_CTE_NAME);

		return QueryStep.builder()
						.cteName(baseStep.getCteName() + INVERTED_DATE_AGGREGATION_CTE_NAME)
						.selects(baseStepSelects.withValidityDate(ColumnDateRange.of(invertedValidityDate)))
						.fromTable(QueryStep.toTableLike(baseStep.getCteName()))
						.predecessors(List.of(baseStep))
						.build();
	}

	private ColumnDateRange getAggregatedValidityDate(DateAggregationDates dateAggregationDates, DateAggregationAction dateAggregationAction, String joinedStepCteName) {

		// see https://www.postgresql.org/docs/current/functions-range.html
		String aggregatingOperator = switch (dateAggregationAction) {
			case MERGE -> " + ";
			case INTERSECT -> " * ";
			default -> throw new IllegalStateException("Unexpected aggregation mode: " + dateAggregationAction);
		};

		String aggregatedExpression = dateAggregationDates.qualify(joinedStepCteName)
														  .getValidityDates().stream()
														  .flatMap(validityDate -> validityDate.toFields().stream())
														  .map(PostgreSqlDateAggregator::createEmptyRangeForNullValues)
														  .collect(Collectors.joining(aggregatingOperator));

		return ColumnDateRange.of(DSL.field(aggregatedExpression))
							  .asValidityDateRange(joinedStepCteName);
	}

	private static String createEmptyRangeForNullValues(Field<?> field) {
		return DSL.when(field.isNull(), DSL.field("'{}'::datemultirange"))
				  .otherwise(field)
				  .toString();
	}

}
