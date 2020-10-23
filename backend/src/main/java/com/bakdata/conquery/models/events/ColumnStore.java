package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Function;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@CPSBase
public interface ColumnStore<T> {

	public static <T> T selectArray(int[] starts, int[] lengths, T values, Function<Integer, T> provider) {
		int length = Arrays.stream(lengths).sum();

		final T out = provider.apply(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			System.arraycopy(values, starts[index], out, pos, lengths[index]);
			pos += lengths[index];
		}

		return out;
	}

	ColumnStore<T> select(int[] starts, int[] length);

	void set(int event, T value);

	T get(int event);

	boolean has(int event);

	int getString(int event);

	long getInteger(int event);

	boolean getBoolean(int event);

	double getReal(int event);

	BigDecimal getDecimal(int event);

	long getMoney(int event);

	int getDate(int event);

	CDateRange getDateRange(int event);

	Object getAsObject(int event);

}
