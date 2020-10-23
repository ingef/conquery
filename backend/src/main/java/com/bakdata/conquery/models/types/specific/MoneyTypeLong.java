package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@CPSType(base = ColumnStore.class, id = "MONEY_LONG")
public class MoneyTypeLong extends CType<Long, Long> {

	@JsonIgnore
	@Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10)
													 .pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());

	public MoneyTypeLong() {
		super(MajorTypeId.MONEY);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Long.SIZE;
	}

	@Override
	public MoneyTypeLong select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Long value) {
		//TODO!
	}

	@Override
	public Long get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}