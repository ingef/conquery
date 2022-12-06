package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.filter.event.DateDistanceFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE);
	}

	@Override
	public void configureFrontend(FEFilterConfiguration.Top f) throws ConceptConfigurationException {
		if (getColumn().getType() != MajorTypeId.DATE) {
			throw new ConceptConfigurationException(getConnector(), "DATE_DISTANCE filter is incompatible with columns of type " + getColumn().getType());
		}

		f.setType(FEFilterType.Fields.INTEGER_RANGE);
	}
	
	@Override
	public FilterNode createFilterNode(Range.LongRange value) {
		return new DateDistanceFilterNode(getColumn(), timeUnit, value);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context, Range.LongRange longRange) {
		final IntOpenHashSet out = new IntOpenHashSet();
		final LocalDate referenceDate = CDate.toLocalDate(context.getDateRestriction().getMaxValue());

		for (CBlock cBlock : context.getBucketManager().getCBlocksForConnector(getConnector())) {

			final Int2ObjectMap<CDateRange> entityDateRanges = cBlock.getColumnIndex(getColumn());

			if(entityDateRanges == null) {
				continue;
			}

			entityDateRanges.int2ObjectEntrySet().stream()
							.filter(kv -> {
								final LocalDate date = kv.getValue().getMax();
								final long between = timeUnit.between(date, referenceDate);
								return longRange.contains(between);
							})
							.mapToInt(Int2ObjectMap.Entry::getIntKey)
							.forEach(out::add);
		}

		return new RequiredEntities(out);
	}
}