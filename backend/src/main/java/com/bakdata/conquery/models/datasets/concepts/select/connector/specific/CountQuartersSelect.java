package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDateRangeAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountQuartersOfDatesAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountQuartersSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity is included when the number of distinct quarters for all events is within a given range.
 * Implementation is specific for DateRanges
 */
@Setter
@Getter
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "COUNT_QUARTERS", base = Select.class)
public class CountQuartersSelect extends Select implements DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;
	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;

	@Override
	public List<ColumnId> getRequiredColumns() {
		if (isSingleColumnDaterange()) {
			return List.of(column);
		}
		return List.of(startColumn, endColumn);
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
	public ResultType getResultType() {
		return ResultType.Primitive.INTEGER;
	}

	@Override
	public SelectConverter<CountQuartersSelect> createConverter() {
		return new CountQuartersSqlAggregator();
	}
}
