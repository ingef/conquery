package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateDistanceAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(id = "DATE_DISTANCE", base = Select.class)
@Getter
@Setter
public class DateDistanceSelect extends SingleColumnSelect {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@JsonCreator
	public DateDistanceSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE, MajorTypeId.DATE_RANGE);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new DateDistanceAggregator(getColumn(), getTimeUnit());
	}

	@Override
	public SqlSelects convertToSqlSelects(SelectContext selectContext) {
		return DateDistanceSqlAggregator.create(this, selectContext).getSqlSelects();
	}

}
