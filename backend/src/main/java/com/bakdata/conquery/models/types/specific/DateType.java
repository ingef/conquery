package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


@CPSType(base = ColumnStore.class, id = "DATES")
@ToString(of = "store")
public class DateType extends CType<Integer> {

	@Getter
	private final ColumnStore<Long> store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateType(ColumnStore<Long> store) {
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

	public static DateType create(int size) {
		return new DateType(IntegerStore.create(size));
	}

	public DateType select(int[] starts, int[] ends) {
		return new DateType(store.select(starts, ends));
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
