package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDateRangeAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDatesAggregator;
import com.bakdata.conquery.sql.conversion.model.select.CountQuartersSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Entity is included when the number of distinct quarters for all events is within a given range.
 * Implementation is specific for DateRanges
 */
@CPSType(id = "COUNT_QUARTERS", base = Select.class)
public class CountQuartersSelect extends SingleColumnSelect {

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@JsonCreator
	public CountQuartersSelect(ColumnId column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		final Column column = getColumn().resolve();
		final MajorTypeId typeId = column.getType();
		return switch (typeId) {
			case DATE_RANGE -> new CountQuartersOfDateRangeAggregator(column);
			case DATE -> new CountQuartersOfDatesAggregator(column);
			default -> throw new IllegalArgumentException(String.format("Column '%s' is not of Date (-Range) Type but '%s'", getColumn(), typeId));
		};
	}

	@Override
	public SqlSelects convertToSqlSelects(SelectContext selectContext) {
		if (getColumn().resolve().getType() == MajorTypeId.DATE_RANGE) {
			throw new UnsupportedOperationException("COUNT_QUARTERS conversion on columns of type DATE_RANGE not implemented yet.");
		}
		return CountQuartersSqlAggregator.create(this, selectContext).getSqlSelects();
	}
}
