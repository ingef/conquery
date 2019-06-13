package com.bakdata.eva.models.query.queryplan.aggregators;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class AgeAggregator extends ColumnAggregator<Long> {
	
	@Getter
	private final Column birthdateColumn;
	private Long age = null;
	private final LocalDate now = LocalDate.now();

	@Override
	public Long getAggregationResult() {
		return age;
	}

	@Override
	public Aggregator<Long> doClone(CloneContext ctx) {
		return new AgeAggregator(getBirthdateColumn());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[] {getBirthdateColumn()};
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		LocalDate birthDate = bucket.has(event, getBirthdateColumn()) ? CDate.toLocalDate(bucket.getDate(event, getBirthdateColumn())) : null;
		
		if(age != null) {
			throw new IllegalStateException("Age has already been calculated. This exception indicates that there are more than one birthdates for this entity");
		}
		age = ChronoUnit.YEARS.between(birthDate, now);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}

}
