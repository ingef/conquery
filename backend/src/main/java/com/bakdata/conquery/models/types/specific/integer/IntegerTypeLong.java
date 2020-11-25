package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=ColumnStore.class, id="INTEGER") @Getter @Setter
public class IntegerTypeLong extends CType<Long> {

	private final long minValue;
	private final long maxValue;

	private final LongStore store;
	
	@JsonCreator
	public IntegerTypeLong(long minValue, long maxValue, LongStore store) {
		super(MajorTypeId.INTEGER);
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.store = store;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Long.SIZE;
	}

	@Override
	public IntegerTypeLong select(int[] starts, int[] length) {
		return new IntegerTypeLong(minValue,  maxValue, store.select(starts, length));
	}

	@Override
	public void set(int event, Long value) {
		store.set(event, value);
	}

	@Override
	public Long get(int event) {
		return store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}