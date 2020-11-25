package com.bakdata.conquery.models.events.stores.date;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

@CPSType(id = "PACKED_DATE_RANGES", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class PackedDateRangeStore extends ColumnStoreAdapter<CDateRange> {

	private final ColumnStore<Long> store;

	@JsonCreator
	public PackedDateRangeStore(ColumnStore<Long> store) {
		this.store = store;
	}

	public static PackedDateRangeStore create(ColumnStore<Long> store) {
		return new PackedDateRangeStore(store);
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
