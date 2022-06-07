package com.bakdata.conquery.models.events.stores.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Store Decimals as longs with a fixed scale.
 */
@CPSType(base = ColumnStore.class, id = "DECIMAL_SCALED")
@Getter
@ToString
public class DecimalTypeScaled implements DecimalStore {

	private final int scale;
	private final IntegerStore subType;

	@JsonCreator
	public DecimalTypeScaled(int scale, IntegerStore subType) {
		this.scale = scale;
		this.subType = subType;
	}

	@Override
	public int getLines() {
		return subType.getLines();
	}

	@Override
	public long estimateEventBits() {
		return subType.estimateEventBits();
	}

	@Override
	public DecimalTypeScaled select(int[] starts, int[] length) {
		return new DecimalTypeScaled(scale, subType.select(starts, length));
	}

	@Override
	public void setDecimal(int event, BigDecimal raw) {
		subType.setInteger(event, unscale(scale, raw).longValue());
	}

	@Override
	public void setNull(int event) {
		subType.setNull(event);
	}

	public static BigInteger unscale(int scale, BigDecimal value) {
		return value.movePointRight(scale).toBigIntegerExact();
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return scale(scale, subType.getInteger(event));
	}

	public static BigDecimal scale(int scale, long value) {
		return BigDecimal.valueOf(value, scale);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}
}