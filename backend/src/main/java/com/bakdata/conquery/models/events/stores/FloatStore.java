package com.bakdata.conquery.models.events.stores;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FloatStore extends ColumnStoreAdapter {

	private final float[] values;

	@Override
	public boolean has(int event) {
		return !Float.isNaN(values[event]);
	}

	@Override
	public double getReal(int event) {
		return values[event];
	}
}
