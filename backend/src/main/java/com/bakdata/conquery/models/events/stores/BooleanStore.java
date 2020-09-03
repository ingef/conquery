package com.bakdata.conquery.models.events.stores;

import java.util.BitSet;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BooleanStore extends ColumnStoreAdapter {

	private final BitSet values;

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public boolean getBoolean(int event) {
		return values.get(event);
	}
}
