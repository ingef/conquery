package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


/**
 * Stores Dates as {@link CDate}s by delegating to a {@link ColumnStore<Integer>} via {@link com.bakdata.conquery.models.events.parser.specific.IntegerParser}.
 */
@CPSType(base = ColumnStore.class, id = "DATES")
@ToString(of = "store")
public class DateStore extends ColumnStore<Integer> {

	@Getter
	private final ColumnStore<Long> store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateStore(ColumnStore<Long> store) {
		this.store = store;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return CDate.toLocalDate(value);
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	public static DateStore create(int size) {
		return new DateStore(IntegerStore.create(size));
	}

	public DateStore doSelect(int[] starts, int[] ends) {
		return new DateStore(store.select(starts, ends));
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			store.set(event, null);
			return;
		}

		store.set(event, value.longValue());
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public CDateRange getDateRange(int event) {
		return CDateRange.exactly(getDate(event));
	}

	@Override
	public Integer get(int event) {
		return getDate(event);
	}

	@Override
	public int getDate(int event) {
		return (int) store.getInteger(event);
	}

	public Object getAsObject(int event) {
		return CDate.toLocalDate(getDate(event));
	}
}
