package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base=ColumnStore.class, id="DECIMAL_BIG_DECIMAL")
@Getter
public class DecimalTypeBigDecimal extends CType<BigDecimal> {

	private final DecimalStore store;

	@JsonCreator
	public DecimalTypeBigDecimal(DecimalStore store) {
		super(MajorTypeId.DECIMAL);
		this.store = store;
	}

	@Override
	public long estimateMemoryBitWidth() {
		return 500;
	}

	@Override
	public DecimalTypeBigDecimal select(int[] starts, int[] length) {
		return new DecimalTypeBigDecimal(store.select(starts, length));
	}

	@Override
	public void set(int event, BigDecimal value) {
		store.set(event, value);
	}

	@Override
	public BigDecimal get(int event) {
		return store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}