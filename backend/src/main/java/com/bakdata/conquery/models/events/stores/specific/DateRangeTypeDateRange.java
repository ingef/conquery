package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Stores {@link CDateRange} as pair of two {@link IntegerDateStore}s.
 */
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
@ToString
public class DateRangeTypeDateRange implements DateRangeStore {

	private final DateStore minStore;
	private final DateStore maxStore;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeDateRange(DateStore minStore, DateStore maxStore) {
		this.minStore = minStore;
		this.maxStore = maxStore;
	}

	@Override
	public int getLines() {
		// they can be unaligned, if one of them is empty.
		return Math.max(minStore.getLines(), maxStore.getLines());
	}

	@Override
	public long estimateEventBits() {
		return minStore.estimateEventBits() + maxStore.estimateEventBits();
	}

	@Override
	public DateRangeTypeDateRange select(int[] starts, int[] length) {
		return new DateRangeTypeDateRange(minStore.select(starts, length), maxStore.select(starts, length));
	}

	@Override
	public void setDateRange(int event, CDateRange raw) {
		if (raw.hasLowerBound()) {
			minStore.setDate(event, raw.getMinValue());
		}
		else {
			minStore.setNull(event);
		}

		if (raw.hasUpperBound()) {
			maxStore.setDate(event, raw.getMaxValue());
		}
		else {
			maxStore.setNull(event);
		}
	}

	@Override
	public void setNull(int event) {
		minStore.setNull(event);
		maxStore.setNull(event);
	}

	@Override
	public CDateRange getDateRange(int event) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (minStore.has(event)) {
			min = minStore.getDate(event);
		}

		if (maxStore.has(event)) {
			max = maxStore.getDate(event);
		}

		return CDateRange.of(min, max);
	}

	@Override
	public boolean has(int event) {
		return minStore.has(event) || maxStore.has(event);
	}
}