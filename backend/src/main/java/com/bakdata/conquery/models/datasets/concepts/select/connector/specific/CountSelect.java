package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "COUNT", base = Select.class)
@NoArgsConstructor
public class CountSelect extends Select {

	@Getter
	@Setter
	private boolean distinct = false;

	@Getter
	@Setter
	@NsIdRef
	private Column distinctByColumn;

	@Getter
	@Setter
	@NsIdRef
	@NotNull
	private Column column;

	@Override
	public Aggregator<?> createAggregator() {
		if (distinct || distinctByColumn != null) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn()), getDistinctByColumn() == null ? getColumn() : getDistinctByColumn());
		}
		return new CountAggregator(getColumn());
	}
}
