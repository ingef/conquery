package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "COUNT", base = Select.class)
@NoArgsConstructor
@Data
public class CountSelect extends Select {

	private boolean distinct = false;

	@NotNull
	private List<ColumnId> distinctByColumn = Collections.emptyList();


	@NotNull
	private ColumnId column;

	@Override
	public Aggregator<?> createAggregator() {
		final Column resolved = getColumn().resolve();
		if (!isDistinct()) {
			return new CountAggregator(resolved);
		}

		if (distinctByColumn != null && !getDistinctByColumn().isEmpty()) {
			return new DistinctValuesWrapperAggregator(new CountAggregator(resolved), getDistinctByColumn().stream().map(ColumnId::resolve).toList());
		}

		return new DistinctValuesWrapperAggregator(new CountAggregator(resolved), List.of(getColumn().resolve()));
	}

	@Nullable
	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();
		out.add(getColumn().resolve());

		if (distinctByColumn != null) {
			out.addAll(getDistinctByColumn().stream().map(ColumnId::resolve).toList());
		}

		return out;
	}

	@Override
	public SelectConverter<CountSelect> createConverter() {
		return new CountSqlAggregator();
	}


	@Override
	public ResultType<?> getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
