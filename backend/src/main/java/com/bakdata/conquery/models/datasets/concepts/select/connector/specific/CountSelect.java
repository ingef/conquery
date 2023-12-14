package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.CountSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "COUNT", base = Select.class)
@NoArgsConstructor
@Data
public class CountSelect extends Select {

	private boolean distinct = false;

	@NsIdRefCollection
	@NotNull
	private List<Column> distinctByColumn = Collections.emptyList();

	@NsIdRef
	@NotNull
	private Column column;

	@Override
	public Aggregator<?> createAggregator() {
		if (!isDistinct()) {
			return new CountAggregator(getColumn());
		}

		if (distinctByColumn != null && !getDistinctByColumn().isEmpty()) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), getDistinctByColumn());
		}

		return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), List.of(getColumn()));
	}

	@Nullable
	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();
		out.add(getColumn());

		if(distinctByColumn != null) {
			out.addAll(getDistinctByColumn());
		}

		return out;
	}

	@Override
	public SqlSelects convertToSqlSelects(SelectContext selectContext) {
		return CountSqlAggregator.create(this, selectContext).getSqlSelects();
	}

}
