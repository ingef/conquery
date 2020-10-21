package com.bakdata.conquery.models.events.stores.date;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "PACKED_DATE_RANGES", base = ColumnStore.class)
@Getter
public class PackedDateRangeStore extends ColumnStoreAdapter<CDateRange> {

	private final IntegerStore store;

	@JsonCreator
	public PackedDateRangeStore(IntegerStore store) {
		this.store = store;
	}

	public static PackedDateRangeStore create(int size) {
		return new PackedDateRangeStore(IntegerStore.create(size));
	}

	public PackedDateRangeStore select(int[] starts, int[] ends) {
		return new PackedDateRangeStore(store.select(starts, ends));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			store.set(event, null);
			return;
		}
		store.set(event, (long) PackedUnsigned1616.pack(value.getMinValue(), value.getMaxValue()));
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public CDateRange get(int event) {
		final Long value = store.get(event);

		return CDateRange.of(PackedUnsigned1616.getLeft(value.intValue()), PackedUnsigned1616.getRight(value.intValue()));
	}

}
