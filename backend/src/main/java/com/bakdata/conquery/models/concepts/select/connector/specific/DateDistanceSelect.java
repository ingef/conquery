package com.bakdata.conquery.models.concepts.select.connector.specific;

import java.time.temporal.ChronoUnit;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregatorNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter @Setter
public class DateDistanceSelect extends SingleColumnSelect {

	private ChronoUnit unit = ChronoUnit.YEARS;

	public DateDistanceSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregatorNode(getColumn(), getUnit());
	}
}
