package com.bakdata.conquery.models.events.stores.date;

import java.time.LocalDate;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

@CPSType(id = "QUARTER_DATES", base = ColumnStore.class)
@Getter
@ToString(of = "store")
public class QuarterDateStore extends ColumnStoreAdapter<CDateRange> {

	private final ColumnStore<Long> store;

	@JsonCreator
	public QuarterDateStore(ColumnStore<Long> store) {
		this.store = store;
	}


	public QuarterDateStore select(int[] starts, int[] ends) {
		return new QuarterDateStore(store.select(starts, ends));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			store.set(event, null);
		}
		else if(value.hasLowerBound()) {
			store.set(event, (long) value.getMinValue());
		}
		else {
			throw new IllegalArgumentException("Cannot store open dates in QuarterStore");
		}
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public CDateRange get(int event) {
		final int begin = (int) store.getInteger(event);
		final LocalDate end = QuarterUtils.getLastDayOfQuarter(begin);

		return CDateRange.of(begin, CDate.ofLocalDate(end));
	}
}
