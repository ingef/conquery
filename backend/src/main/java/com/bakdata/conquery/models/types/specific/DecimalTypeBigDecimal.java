package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(base= CType.class, id="DECIMAL_BIG_DECIMAL")
@Getter
public class DecimalTypeBigDecimal extends CType<BigDecimal> {

	private final CType<BigDecimal> store;

	@JsonCreator
	public DecimalTypeBigDecimal(CType<BigDecimal> store) {
		super(MajorTypeId.DECIMAL);
		this.store = store;
	}

	@Override
	public long estimateEventBytes() {
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