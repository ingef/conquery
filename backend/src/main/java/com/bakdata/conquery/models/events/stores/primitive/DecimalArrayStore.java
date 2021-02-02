package com.bakdata.conquery.models.events.stores.primitive;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Stores fixed point decimals as BigDecimals. Null is Null.
 */
@CPSType(id = "DECIMALS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class DecimalArrayStore implements DecimalStore {

	private final BigDecimal[] values;

	@JsonCreator
	public DecimalArrayStore(BigDecimal[] values) {
		this.values = values;
	}

	public static DecimalArrayStore create(int size) {
		return new DecimalArrayStore(new BigDecimal[size]);
	}

	@Override
	public int getLines() {
		return values.length;
	}

	@Override
	public long estimateEventBits() {
		return 256; // Source: http://javamoods.blogspot.com/2009/03/how-big-is-bigdecimal.html
	}

	public DecimalArrayStore select(int[] starts, int[] ends) {
		return new DecimalArrayStore(ColumnStore.selectArray(starts, ends, values, BigDecimal[]::new));
	}

	@Override
	public void set(int event, BigDecimal value) {
		if(value == null){
			values[event] = null;
			return;
		}

		values[event] = value;
	}

	@Override
	public boolean has(int event) {
		return values[event] != null;
	}


	@Override
	public BigDecimal get(int event) {
		return getDecimal(event);
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return values[event];
	}
}
