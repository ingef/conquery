package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "QUARTER_DATES", base = ColumnStore.class)
@Getter
public class QuarterDateStore extends ColumnStoreAdapter<QuarterDateStore> {

	private final ColumnStore<?> store;

	@JsonCreator
	public QuarterDateStore(ColumnStore<?> store) {
		this.store = store;
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public QuarterDateStore merge(List<? extends QuarterDateStore> stores) {

		final List<ColumnStore> collect = stores.stream().map(QuarterDateStore::getStore).collect(Collectors.toList());

		final ColumnStore values = collect.get(0).merge(collect);

		return new QuarterDateStore(values);
	}

	@Override
	public CDateRange getDateRange(int event) {
		final int begin = (int) store.getInteger(event);
		final LocalDate end = QuarterUtils.getLastDayOfQuarter(begin);

		return CDateRange.of(begin, CDate.ofLocalDate(end));
	}


	@Override
	public Object getAsObject(int event) {
		return CDate.toLocalDate(getDate(event));
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
