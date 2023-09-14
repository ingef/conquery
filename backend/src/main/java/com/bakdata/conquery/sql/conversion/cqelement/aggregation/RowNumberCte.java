package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.ArrayList;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Getter;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Concept for date range inversion taken from <a href="https://explainextended.com/2009/11/09/inverting-date-ranges/">Inverting date ranges</a>.
 */
@Getter
class RowNumberCte extends DateAggregationCte {

	public static final String ROW_NUMBER_FIELD_NAME = "row_number";
	private final DateAggregationStep cteStep;

	public RowNumberCte(DateAggregationStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		Field<Object> primaryColumn = context.getPrimaryColumn();

		ColumnDateRange aggregatedValidityDate = context.getDateAggregationDates().getValidityDates().get(0);
		Field<Integer> rowNumber = DSL.rowNumber().over(DSL.partitionBy(primaryColumn).orderBy(aggregatedValidityDate.getStart()))
									  .as(ROW_NUMBER_FIELD_NAME);

		ArrayList<SqlSelect> selects = new ArrayList<>(context.getCarryThroughSelects());
		selects.add(new FieldWrapper(rowNumber));

		Selects rowNumberSelects = new Selects(
				primaryColumn,
				Optional.of(aggregatedValidityDate),
				selects
		);

		return QueryStep.builder()
						.selects(rowNumberSelects);
	}

}
