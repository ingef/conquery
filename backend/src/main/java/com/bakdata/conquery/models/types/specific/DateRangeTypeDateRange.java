package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
public class DateRangeTypeDateRange extends CType<CDateRange> {

	private final CType<Long> store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeDateRange(CType<Long> store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}

	@Override
	public long estimateMemoryFieldSize() {
		return store.estimateMemoryFieldSize() * 2;
	}

	@Override
	public DateRangeTypeDateRange select(int[] starts, int[] length) {
		return new DateRangeTypeDateRange(store.select(starts, length));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			store.set(left(event), null);
			store.set(right(event), null);
			return;
		}

		if (value.hasLowerBound()) {
			store.set(left(event), (long) value.getMinValue());
		}
		else {
			store.set(left(event), null);
		}

		if (value.hasUpperBound()) {
			store.set(right(event), (long) value.getMaxValue());
		}
		else {
			store.set(right(event), null);
		}
	}

	private static int left(int event) {
		return event * 2;
	}

	private static int right(int event) {
		return left(event) + 1;
	}

	@Override
	public CDateRange get(int event) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (store.has(event)) {
			min = store.get(left(event)).intValue();
		}

		if (store.has(right(event))) {
			max = store.get(right(event)).intValue();
		}

		return CDateRange.of(min, max);
	}

	@Override
	public boolean has(int event) {
		return store.has(left(event)) && store.has(right(event));
	}
}