package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DATES", base = ColumnStore.class)
@Getter
public class DateStore extends ColumnStoreAdapter<DateStore> {

	private final ColumnStore<?> store;

	@JsonCreator
	public DateStore(ColumnStore<?> store) {
		this.store = store;
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public DateStore merge(List<? extends DateStore> stores) {

		final List<ColumnStore> collect = stores.stream().map(DateStore::getStore).collect(Collectors.toList());

		final ColumnStore values = collect.get(0).merge(collect);

		return new DateStore(values);
	}

	@Override
	public CDateRange getDateRange(int event) {
		return CDateRange.exactly(getDate(event));
	}

	@Override
	public int getDate(int event) {
		return (int) store.getInteger(event);
	}

	@Override
	public Object getAsObject(int event) {
		return CDate.toLocalDate(getDate(event));
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
