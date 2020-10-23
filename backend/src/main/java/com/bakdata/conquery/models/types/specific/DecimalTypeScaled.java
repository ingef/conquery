package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="DECIMAL_SCALED") @ToString
public class DecimalTypeScaled extends CType<BigDecimal, Number> {

	@Getter @ToString.Include
	private final int scale;
	@Getter @ToString.Include
	private final CType subType;
	
	@JsonCreator
	public DecimalTypeScaled(int scale, CType subType) {
		super(MajorTypeId.DECIMAL);
		this.scale = scale;
		this.subType = subType;
	}

	@Override
	public ColumnStore createStore(int size) {
		return DecimalStore.create(size);
	}

	@Override
	public BigDecimal createScriptValue(Number value) {
		return scale(scale, (Long)subType.createScriptValue(value));
	}

	public static BigInteger unscale(int scale, BigDecimal value) {
		return value.movePointRight(scale).toBigIntegerExact();
	}
	
	public static BigDecimal scale(int scale, long value) {
		return BigDecimal.valueOf(value, scale);
	}
	
	@Override
	public String toString() {
		return "DecimalTypeScaled[numberType=" + subType + "]";
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return subType.estimateMemoryBitWidth();
	}

	@Override
	public ColumnStore<Number> select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Number value) {

	}

	@Override
	public Number get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}