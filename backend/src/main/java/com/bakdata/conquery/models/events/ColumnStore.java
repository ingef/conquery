package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @param <T> the Uppermost format of this type. NOT the storage type.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@CPSBase
public abstract class ColumnStore<T> {

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

	public abstract ColumnStore<T> select(int[] starts, int[] length);

	/**
	 * Set the event. If null, the store will store a null value.
	 */
	public abstract void set(int event, @CheckForNull T value);

	@NotNull
	public abstract T get(int event);

	public abstract boolean has(int event);

	public int getString(int event) {
		return (int) get(event);
	}

	public long getInteger(int event) {
		return (long) get(event);
	}

	public boolean getBoolean(int event) {
		return (boolean) get(event);
	}

	public double getReal(int event) {
		return (double) get(event);
	}

	public BigDecimal getDecimal(int event) {
		return (BigDecimal) get(event);
	}

	public long getMoney(int event) {
		return (long) get(event);
	}

	public int getDate(int event) {
		return (int) get(event);
	}

	public CDateRange getDateRange(int event) {
		return (CDateRange) get(event);
	}

	public Object getAsObject(int event) {
		return get(event);
	}
}
