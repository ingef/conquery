package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

@CPSType(id = "DOUBLES", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class DoubleStore extends ColumnStore<Double> {

	private final double[] values;

	@JsonCreator
	public DoubleStore(double[] values) {
		this.values = values;
	}

	public static DoubleStore create(int size) {
		return new DoubleStore(new double[size]);
	}

	@Override
	public long estimateEventBytes() {
		return Double.BYTES;
	}

	public DoubleStore select(int[] starts, int[] ends) {
		return new DoubleStore(ColumnStore.selectArray(starts, ends, values, double[]::new));
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
		return values[event];
	}
}
