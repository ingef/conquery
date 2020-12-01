package com.bakdata.conquery.models.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.math.LongMath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class CType<JAVA_TYPE> implements MajorTypeIdHolder {

	@JsonIgnore
	@NotNull
	private transient final MajorTypeId typeId;

	private int lines = 0;
	private int nullLines = 0;

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

	public Object createPrintValue(JAVA_TYPE value) {
		return value != null ? createScriptValue(value) : "";
	}

	public Object createScriptValue(JAVA_TYPE value) {
		return value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}


	public long estimateMemoryConsumption() {
		long width = estimateEventBytes();

		return LongMath.divide(
				(lines - nullLines) * width + nullLines * Math.min(Long.SIZE, width),
				8, RoundingMode.CEILING
		);
	}

	public abstract long estimateEventBytes();

	public long estimateTypeSize() {
		return 0;
	}

	public abstract CType<JAVA_TYPE> select(int[] starts, int[] lengths);

	/**
	 * Set the event. If null, the store will store a null value.
	 */
	public abstract void set(int event, @CheckForNull JAVA_TYPE value);

	public abstract boolean has(int event);

	public int getString(int event) {
		return (int) get(event);
	}

	public abstract JAVA_TYPE get(int event);

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
