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
	private static final double nullValue = Double.NaN;

	@JsonCreator
	public DoubleArrayStore(double[] values) {
		this.values = values;
	}

	public static DoubleArrayStore create(int size) {
		return new DoubleArrayStore(new double[size]);
	}

	@Override
	public DoubleArrayStore createDescription() {
		return ColumnStore.emptyCopy(this);
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
	public void setReal(int event, double value) {
		values[event] = value;
	}

	@Override
	public void setNull(int event) {
		values[event] = nullValue;
	}

	@Override
	public boolean has(int event) {
		return !Double.isNaN(values[event]);
	}


	@Override
	public double getReal(int event) {
		return values[event];
	}
}
