package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DurationSumAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.DurationSumSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "DURATION_SUM", base = Select.class)
@JsonIgnoreProperties("categorical")
public class DurationSumSelect extends Select implements DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;

	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;

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

		if (distinctBy != null) {
			out.addAll(distinctBy);
		}
		return out;
	}

	@Override
	public Aggregator<?> createAggregator() {
		DurationSumAggregator aggregator = new DurationSumAggregator(getColumn().resolve());

		if (distinctBy == null) {
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
		//TODO apply distinctBy (though needs to be done once other branches
		return new DurationSumSelectConverter();
	}
}
