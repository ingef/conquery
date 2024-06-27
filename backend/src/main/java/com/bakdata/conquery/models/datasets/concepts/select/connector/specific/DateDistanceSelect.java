package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter
@Setter
public class DateDistanceSelect extends SingleColumnSelect {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@JsonCreator
	public DateDistanceSelect(ColumnId column) {
		super(column);
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregator(getColumn().resolve(), getTimeUnit());
	}

	@Override
	public SelectConverter<DateDistanceSelect> createConverter() {
		return new DateDistanceSqlAggregator();
	}

	@Override
	public ResultType<?> getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
