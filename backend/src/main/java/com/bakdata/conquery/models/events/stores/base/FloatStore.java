package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

@CPSType(id = "FLOATS", base = CType.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class FloatStore extends CType<Double> {

	private final float[] values;

	@JsonCreator
	public FloatStore(float[] values) {
		super(MajorTypeId.REAL);
		this.values = values;
	}

	public static FloatStore create(int size) {
		return new FloatStore(new float[size]);
	}

	@Override
	public long estimateEventBytes() {
		return Float.BYTES;
	}

	public FloatStore select(int[] starts, int[] ends) {
		return new FloatStore(CType.selectArray(starts, ends, values, float[]::new));
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
