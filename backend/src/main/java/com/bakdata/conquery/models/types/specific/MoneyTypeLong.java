package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@CPSType(base = ColumnStore.class, id = "MONEY_LONG")
@Getter
public class MoneyTypeLong extends CType<Long> {

	@JsonIgnore
	@Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10)
													 .pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());

	private final LongStore store;

	@JsonCreator
	public MoneyTypeLong(LongStore store) {
		super(MajorTypeId.MONEY);
		this.store = store;
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Long.SIZE;
	}

	@Override
	public MoneyTypeLong select(int[] starts, int[] length) {
		return new MoneyTypeLong(store.select(starts, length));
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