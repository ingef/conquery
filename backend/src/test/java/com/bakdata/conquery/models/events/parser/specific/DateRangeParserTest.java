package com.bakdata.conquery.models.events.parser.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import org.junit.jupiter.api.Test;

class DateRangeParserTest {
	@Test
	public void onlyClosed() {
		final DateRangeParser parser = new DateRangeParser(new ParserConfig());

		List.of(CDateRange.of(10,11), CDateRange.exactly(10))
			.forEach(parser::addLine);

		final ColumnStore<CDateRange> actual = parser.decideType();

		assertThat(actual).isInstanceOf(DateRangeTypeDateRange.class);

		assertThat(((DateRangeTypeDateRange) actual).getMinStore().getStore())
				.isInstanceOfAny(ByteStore.class, RebasingStore.class);
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
}