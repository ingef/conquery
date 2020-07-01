package com.bakdata.conquery.models.concepts.filters.specific;

import java.time.temporal.ChronoUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.query.filter.event.DateDistanceFilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter @Setter @Slf4j
@CPSType(id="DATE_DISTANCE", base=Filter.class)
public class DateDistanceFilter extends SingleColumnFilter<Range.LongRange> {

	@NotNull
	private ChronoUnit timeUnit = ChronoUnit.YEARS;

	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.INTEGER_RANGE);
	}

	@Valid()
	public boolean columnIsDateBased(){
		if(getColumn() == null){
			return false;
		}

		if(!getColumn().getType().isDateCompatible()){
			log.error("Column[{}] is not date based", getColumn().getId());
			return false;
		}

		return true;
	}


	@Override
	public DateDistanceFilterNode createAggregator(Range.LongRange value) {
		return new DateDistanceFilterNode(timeUnit, value, getColumn());
	}
}