package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateUnionAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.DateUnionSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "DATE_UNION", base = Select.class)
@JsonIgnoreProperties("categorical")
public class DateUnionSelect extends Select implements DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;
	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;

	@Override
	public List<ColumnId> getRequiredColumns() {
		if (column != null) {
			return List.of(column);
		}
		return List.of(startColumn, endColumn);
	}

	@Override
	public Aggregator<?> createAggregator() {
		// TODO fix this for 2 columns
		return new DateUnionAggregator(getColumn().resolve());
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);
	}

	@Override
	public SelectConverter<DateUnionSelect> createConverter() {
		return new DateUnionSelectConverter();
	}
}
