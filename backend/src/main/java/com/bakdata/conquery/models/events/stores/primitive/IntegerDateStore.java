package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


/**
 * Stores Dates as {@link CDate}s by delegating to a {@link ColumnStore<Integer>} via {@link com.bakdata.conquery.models.events.parser.specific.IntegerParser}.
 */
@CPSType(base = ColumnStore.class, id = "DATES")
@ToString(of = "store")
public class IntegerDateStore implements DateStore {

	@Getter
	private final IntegerStore store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public IntegerDateStore(IntegerStore store) {
		this.store = store;
	}

	public static IntegerDateStore create(int size) {
		return new IntegerDateStore(IntArrayStore.create(size));
	}

	@Override
	public int getLines() {
		return store.getLines();
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	public IntegerDateStore select(int[] starts, int[] ends) {
		return new IntegerDateStore(store.select(starts, ends));
	}

	@Override
	public void setDate(int event, int value) {
		store.setInteger(event, value);
	}

	@Override
	public void setNull(int event) {
		store.setNull(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}


	@Override
	public int getDate(int event) {
		return (int) store.getInteger(event);
	}

}
