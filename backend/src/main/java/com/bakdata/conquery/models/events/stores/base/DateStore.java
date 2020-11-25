package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;


@CPSType(base = ColumnStore.class, id = "DATES")
@Getter
@Setter
public class DateStore extends CType<Integer> {

	private final ColumnStore<Long> store;

	@JsonCreator
	public DateStore(ColumnStore<Long> store) {
		super(MajorTypeId.DATE);
		this.store = store;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return CDate.toLocalDate(value);
	}

	@Override
	public long estimateMemoryFieldSize() {
		return Integer.BYTES;
	}

	public static DateStore create(int size) {
		return new DateStore(IntegerStore.create(size));
	}

	public DateStore select(int[] starts, int[] ends) {
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
		return CDateRange.exactly(get(event));
	}

	@Override
	public Integer get(int event) {
		return store.get(event).intValue();
	}

	@Override
	public Object getAsObject(int event) {
		return CDate.toLocalDate(getDate(event));
	}
}
