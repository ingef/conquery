package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@CPSType(base= ColumnStore.class, id="INTEGER") @Getter @Setter
@ToString(of = "store")
public class IntegerType extends ColumnStore<Long> {


	private final ColumnStore<Long> store;
	
	@JsonCreator
	public IntegerType(ColumnStore<Long> store) {
		super(MajorTypeId.INTEGER);
		this.store = store;
	}

	@Override
	public long estimateEventBytes() {
		return Long.SIZE;
	}

	@Override
	public IntegerType select(int[] starts, int[] length) {
		return new IntegerType(store.select(starts, length));
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