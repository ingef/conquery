package com.bakdata.conquery.models.events.stores;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString
public abstract class ColumnStore<JAVA_TYPE> {

	private int lines = 0; // todo this is only used for estimating size, extract it somehow.

	/**
	 * Helper method to select partitions of an array. Resulting array is of length sum(lengths). Incoming type T has to be of ArrayType or this will fail.
	 *
	 * @param provider method to allocate an Array of the required size.
	 */
	public static <T> T selectArray(int[] starts, int[] lengths, T values, Int2ObjectFunction<T> provider) {
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

	/**
	 * Calculate estimate of total bytes used by this store.
	 */
	public long estimateMemoryConsumptionBytes() {
		long bits = estimateEventBits();

		return Math.floorDiv(getLines() * bits, Byte.SIZE);
	}

	/**
	 * Number of bits required to store a single value.
	 */
	public abstract long estimateEventBits();

	/**
	 * Bytes required to store auxilary data.
	 */
	public long estimateTypeSizeBytes() {
		return 0;
	}

	/**
	 * Select the partition of this store.
	 * The returning store has to accept queries up to {@code sum(lenghts)}, values may not be reordered.
	 */
	public abstract ColumnStore<JAVA_TYPE> select(int[] starts, int[] lengths);

	/**
	 * Create an empty store that's only a description of the transformation.
	 */
	public ColumnStore<JAVA_TYPE> createDescription() {
		final ColumnStore<JAVA_TYPE> select = select(new int[0], new int[0]);
		select.setLines(getLines());
		return select;
	}

	/**
	 * Set the event. If null, the store will store a null value, making {@link #has(int)} return false.
	 */
	public abstract void set(int event, @CheckForNull JAVA_TYPE value);

	/**
	 * Generic getter for storage. May not be called when {@link #has(int)} is false.
	 */
	public abstract JAVA_TYPE get(int event);

	/**
	 * Test if the store has the event.
	 */
	public abstract boolean has(int event);


	public int getString(int event) {
		throw NotImplemented();
	}

	public long getInteger(int event) {
		throw NotImplemented();
	}

	public boolean getBoolean(int event) {
		throw NotImplemented();
	}

	public IllegalStateException NotImplemented() {
		return new IllegalStateException(String.format("%s does not implement this method", getClass().getSimpleName()));
	}

	public double getReal(int event) {
		throw NotImplemented();
	}

	public BigDecimal getDecimal(int event) {
		throw NotImplemented();
	}

	public long getMoney(int event) {
		throw NotImplemented();
	}

	public int getDate(int event) {
		throw NotImplemented();
	}

	public CDateRange getDateRange(int event) {
		throw NotImplemented();
	}

	 @JsonIgnore
	public boolean isEmpty() {
		return getLines() == 0;
	}
}
