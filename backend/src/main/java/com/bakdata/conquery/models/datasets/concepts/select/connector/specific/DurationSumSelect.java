package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.TwoColumnDurationSumAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.DurationSumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@CPSType(id = "DURATION_SUM", base = Select.class)
@JsonIgnoreProperties("categorical")
public class DurationSumSelect extends Select implements DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;

	@Nullable
	private ColumnId startColumn, endColumn;

	private List<ColumnId> distinctBy;

	@Override
	public List<ColumnId> getRequiredColumns() {
		List<ColumnId> out = new ArrayList<>();

		if (column != null) {
			out.add(column);
		}
		else {
			out.add(startColumn);
			out.add(endColumn);
		}

		if (hasDistinct()) {
			out.addAll(distinctBy);
		}
		return out;
	}

	@JsonIgnore
	private boolean hasDistinct() {
		return distinctBy != null && !distinctBy.isEmpty();
	}

	@Override
	public Aggregator<?> createAggregator() {
		ColumnAggregator<?> aggregator = getColumn() != null ? new DurationSumAggregator(getColumn().resolve())
															 : new TwoColumnDurationSumAggregator(startColumn.resolve(), endColumn.resolve());

		if (!hasDistinct()) {
			return aggregator;
		}

		return new DistinctValuesWrapperAggregator<>(aggregator, distinctBy.stream().map(ColumnId::resolve).toList());
	}

	@Override
	public ResultType getResultType() {
		return ResultType.Primitive.INTEGER;
	}

	@Override
	public SelectConverter<DurationSumSelect> createConverter() {
		//TODO apply distinctBy (though needs to be done once other branches are merged)
		return new DurationSumSqlAggregator();
	}
}
