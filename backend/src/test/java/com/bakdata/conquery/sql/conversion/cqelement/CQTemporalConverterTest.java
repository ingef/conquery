package com.bakdata.conquery.sql.conversion.cqelement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.*;

import java.sql.Date;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalRelationMode;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSelector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class CQTemporalConverterTest {

	private final SqlFunctionProvider functionProvider = new HanaSqlFunctionProvider();
	private final Field<Date> start = field(name("start"), Date.class);
	private final Field<Date> end = field(name("end"), Date.class);
	private final ColumnDateRange index = ColumnDateRange.of(start, end);

	@Test
	void createsBoundedAfterRangeFromInclusiveLegacyDays() {
		Field<Date> anchor = functionProvider.addDays(end, inline(-1));
		ColumnDateRange actual = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.After(new Range.IntegerRange(5, 8)),
				TemporalSelector.ANY,
				functionProvider
		);

		assertRange(
				actual,
				functionProvider.addDays(anchor, inline(5)),
				functionProvider.addDays(anchor, inline(9))
		);
	}

	@Test
	void createsLowerBoundedAfterRange() {
		ColumnDateRange actual = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.After(new Range.IntegerRange(5, null)),
				TemporalSelector.EARLIEST,
				functionProvider
		);

		assertRange(actual, functionProvider.addDays(start, inline(5)), functionProvider.getMaxDateExpression());
	}

	@Test
	void createsUpperBoundedAfterRange() {
		Field<Date> anchor = functionProvider.addDays(end, inline(-1));
		ColumnDateRange actual = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.After(new Range.IntegerRange(null, 8)),
				TemporalSelector.ALL,
				functionProvider
		);

		assertRange(actual, anchor, functionProvider.addDays(anchor, inline(9)));
	}

	@Test
	void createsBoundedBeforeRangeFromInclusiveLegacyDays() {
		ColumnDateRange actual = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.Before(new Range.IntegerRange(5, 8)),
				TemporalSelector.ANY,
				functionProvider
		);

		assertRange(
				actual,
				functionProvider.addDays(start, inline(-8)),
				functionProvider.addDays(start, inline(-4))
		);
	}

	@Test
	void createsOneSidedBeforeRanges() {
		ColumnDateRange lowerBounded = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.Before(new Range.IntegerRange(5, null)),
				TemporalSelector.EARLIEST,
				functionProvider
		);
		ColumnDateRange upperBounded = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.Before(new Range.IntegerRange(null, 8)),
				TemporalSelector.ANY,
				functionProvider
		);

		assertRange(
				lowerBounded,
				functionProvider.getMinDateExpression(),
				functionProvider.addDays(start, inline(-4))
		);
		assertRange(
				upperBounded,
				functionProvider.addDays(start, inline(-8)),
				functionProvider.addDays(start, inline(1))
		);
	}

	@Test
	void createsUnrestrictedAndWhileRanges() {
		Field<Date> afterAnchor = functionProvider.addDays(end, inline(-1));
		ColumnDateRange after = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.After(new Range.IntegerRange(null, null)),
				TemporalSelector.ANY,
				functionProvider
		);
		ColumnDateRange before = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.Before(new Range.IntegerRange(null, null)),
				TemporalSelector.ANY,
				functionProvider
		);
		ColumnDateRange whileRange = CQTemporalConverter.temporalBounds(
				index,
				new TemporalRelationMode.While(),
				TemporalSelector.ANY,
				functionProvider
		);

		assertRange(after, afterAnchor, functionProvider.getMaxDateExpression());
		assertRange(before, functionProvider.getMinDateExpression(), functionProvider.addDays(start, inline(1)));
		assertRange(whileRange, start, end);
	}

	private static void assertRange(ColumnDateRange actual, Field<Date> expectedStart, Field<Date> expectedEnd) {
		assertThat(actual.getStart().toString()).isEqualTo(expectedStart.toString());
		assertThat(actual.getEnd().toString()).isEqualTo(expectedEnd.toString());
	}
}
