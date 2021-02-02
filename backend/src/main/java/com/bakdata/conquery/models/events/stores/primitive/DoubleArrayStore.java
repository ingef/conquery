package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Stores values as 64bit doubles, where NaN is null.
 */
@CPSType(id = "DOUBLES", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class DoubleArrayStore implements RealStore {

	private final double[] values;

	@JsonCreator
	public DoubleArrayStore(double[] values) {
		this.values = values;
	}

	public static DoubleArrayStore create(int size) {
		return new DoubleArrayStore(new double[size]);
	}

	@Override
	public int getLines() {
		return values.length;
	}

	@Override
	public long estimateEventBits() {
		return Double.SIZE;
	}

	public DoubleArrayStore select(int[] starts, int[] ends) {
		return new DoubleArrayStore(ColumnStore.selectArray(starts, ends, values, double[]::new));
	}

	@Override
	public void set(int event, Double value) {
		if(value == null){
			values[event] = Double.NaN;
			return;
		}

		values[event] = value;
	}

	@Override
	public boolean has(int event) {
		return !Double.isNaN(values[event]);
	}

	@Override
	public Double get(int event) {
		return getReal(event);
	}

	@Override
	public double getReal(int event) {
		return values[event];
	}
}
