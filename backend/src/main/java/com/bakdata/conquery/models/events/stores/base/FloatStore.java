package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "FLOATS", base = ColumnStore.class)
@Getter
public class FloatStore extends ColumnStoreAdapter<Double> {

	private final float[] values;

	@JsonCreator
	public FloatStore(float[] values) {
		this.values = values;
	}

	public static FloatStore create(int size) {
		return new FloatStore(new float[size]);
	}

	public FloatStore select(int[] starts, int[] ends) {
		return new FloatStore(ColumnStore.selectArray(starts, ends, values, float[]::new));
	}

	@Override
	public void set(int event, Double value) {
		if(value == null){
			values[event] = Float.NaN;
			return;
		}

		values[event] = value.floatValue();
	}

	@Override
	public boolean has(int event) {
		return !Float.isNaN(values[event]);
	}

	@Override
	public Double get(int event) {
		return (double) values[event];
	}
}
