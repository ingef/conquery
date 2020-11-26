package com.bakdata.conquery.models.types.specific.integer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@CPSType(base=ColumnStore.class, id="INTEGER") @Getter @Setter
@ToString(of = "store")
public class IntegerTypeLong extends CType<Long> {


	private final ColumnStore<Long> store;
	
	@JsonCreator
	public IntegerTypeLong(ColumnStore<Long> store) {
		super(MajorTypeId.INTEGER);
		this.store = store;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Long.SIZE;
	}

	@Override
	public IntegerTypeLong select(int[] starts, int[] length) {
		return new IntegerTypeLong(store.select(starts, length));
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