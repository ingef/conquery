package com.bakdata.conquery.models.types.specific.daterange;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "DATE_RANGE_2UINT16")
@Getter
@Setter
public class DateRangeTypePacked extends CType<CDateRange> {

	private final ColumnStore<Long> store;

	@JsonCreator
	public DateRangeTypePacked(ColumnStore<Long> store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
	}

	public DateRangeTypePacked select(int[] starts, int[] ends) {
		return new DateRangeTypePacked(store.select(starts, ends));
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


	@Override
	public Object createScriptValue(CDateRange value) {
		return value.toString();
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Integer.SIZE;
	}

}