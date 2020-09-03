package com.bakdata.conquery.models.events.stores;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DoubleStore extends ColumnStoreAdapter {

	private final double[] values;

	@Override
	public boolean has(int event) {
		return !Double.isNaN(values[event]);
	}

	@Override
	public double getReal(int event) {
		return values[event];
	}
}
