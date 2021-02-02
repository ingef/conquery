package com.bakdata.conquery.models.events.stores.root;

import java.util.Arrays;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;

/**
 * Class representing the data of a single {@link com.bakdata.conquery.models.datasets.Column} of a {@link com.bakdata.conquery.models.events.Bucket}. {@code JAVA_TYPE} is the outermost used for type-safe set/get, usage is to be avoided.
 *
 * Internal representation of data is completely transparent to the caller. get may only be called if has is true.
 *
 * @param <JAVA_TYPE> The outer-most type of the store, with which it is compatible.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface ColumnStore {

	@JsonIgnore
	public abstract int getLines();

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


	public default Object createPrintValue(int event) {
		if(!has(event)) {
			return "";
		}

		return createScriptValue(event);
	}

	public Object createScriptValue(int event);

	/**
	 * Calculate estimate of total bytes used by this store.
	 */
	public default long estimateMemoryConsumptionBytes() {
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
	public default long estimateTypeSizeBytes() {
		return 0;
	}



	/**
	 * Select the partition of this store.
	 * The returning store has to accept queries up to {@code sum(lenghts)}, values may not be reordered.
	 */
	public abstract  <T extends ColumnStore> T select(int[] starts, int[] lengths);



	/**
	 * Create an empty store that's only a description of the transformation.
	 */
	public default ColumnStore createDescription() {
		return this.select(new int[0], new int[0]);
	}

	/**
	 * Set the event. If null, the store will store a null value, making {@link #has(int)} return false.
	 */
	public abstract void set(int event, @CheckForNull Object value);


	/**
	 * Test if the store has the event.
	 */
	public abstract boolean has(int event);

	public void setNull(int event);


	 @JsonIgnore
	public default boolean isEmpty() {
		return getLines() == 0;
	}
}
