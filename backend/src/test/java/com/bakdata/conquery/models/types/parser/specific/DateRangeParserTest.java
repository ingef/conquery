package com.bakdata.conquery.models.types.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.types.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.types.specific.IntegerType;
import com.bakdata.conquery.util.PackedUnsigned1616;
import org.junit.jupiter.api.Test;

class DateRangeParserTest {
	@Test
	public void onlyClosed() {
		final DateRangeParser parser = new DateRangeParser(new ParserConfig());

		List.of(CDateRange.of(10,11), CDateRange.exactly(10))
			.forEach(parser::registerValue);

		final CType<CDateRange> actual = parser.decideType();

		assertThat(actual).isInstanceOf(DateRangeTypeDateRange.class);
		assertThat(((DateRangeTypeDateRange) actual).getStore()).isInstanceOf(IntegerType.class);

	}


	@Test
	public void notOnlyClosed() {
		final DateRangeParser parser = new DateRangeParser(new ParserConfig());

		List.of(CDateRange.of(10,11), CDateRange.exactly(10), CDateRange.atMost(10))
			.forEach(parser::registerValue);

		assertThat(parser.decideType()).isInstanceOf(DateRangeTypeDateRange.class);
	}

	@Test
	public void onlyQuarters() {
		final DateRangeParser parser = new DateRangeParser(new ParserConfig());

		List.of(CDateRange.of(QuarterUtils.getFirstDayOfQuarter(2011,1), QuarterUtils.getLastDayOfQuarter(2011,1)))
			.forEach(parser::registerValue);

		assertThat(parser.decideType()).isInstanceOf(DateRangeTypeQuarter.class);
	}

	@Test
	public void notPacked() {
		final DateRangeParser parser = new DateRangeParser(new ParserConfig());

		List.of(CDateRange.of(10, PackedUnsigned1616.MAX_VALUE + 12))
			.forEach(parser::registerValue);

		assertThat(parser.decideType()).isInstanceOf(DateRangeTypeDateRange.class);
	}


}