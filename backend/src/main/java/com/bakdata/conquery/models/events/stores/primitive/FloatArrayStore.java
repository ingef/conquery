package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Stores values as floats, where NaN is null.
 */
@CPSType(id = "FLOATS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class FloatArrayStore implements RealStore {

	private final float[] values;
	private static final float nullValue = Float.NaN;

	@Override
	public int getLines() {
		return values.length;
	}

	@JsonCreator
	public FloatArrayStore(float[] values) {
		this.values = values;
	}

	public static FloatArrayStore create(int size) {
		return new FloatArrayStore(new float[size]);
	}


	@Override
	public FloatArrayStore createDescription() {
		return ColumnStore.emptyCopy(this);
	}

	@Override
	public long estimateEventBits() {
		return Float.SIZE;
	}

	public FloatArrayStore select(int[] starts, int[] ends) {
		return new FloatArrayStore(ColumnStore.selectArray(starts, ends, values, float[]::new));
	}

	@Override
	public void setReal(int event, double value) {
		values[event] = (float) value;
	}

	@Override
	public void setNull(int event) {
		values[event] = nullValue;
	}

	@Override
	public boolean has(int event) {
		return !Float.isNaN(values[event]);
	}

	@Override
	public double getReal(int event) {
		return values[event];
	}
}
