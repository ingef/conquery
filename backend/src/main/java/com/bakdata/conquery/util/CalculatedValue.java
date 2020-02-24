package com.bakdata.conquery.util;

import java.util.function.Supplier;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 *
 * @param <T>
 */
@RequiredArgsConstructor @ToString(of="value")
public class CalculatedValue<T> {
	@NonNull
	private Supplier<T> calculation;

	private T value;

	public T getValue() {
		if(!isCalculated()) {
			value = calculation.get();
		}

		return value;
	}

	public boolean isCalculated() {
		return value != null;
	}
}
