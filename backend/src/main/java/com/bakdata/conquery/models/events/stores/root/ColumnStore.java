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
public abstract class ColumnStore<JAVA_TYPE> {

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


	public abstract ColumnStore<JAVA_TYPE> doSelect(int[] starts, int[] lengths);

	/**
	 * Select the partition of this store.
	 * The returning store has to accept queries up to {@code sum(lenghts)}, values may not be reordered.
	 */
	public <T extends ColumnStore<JAVA_TYPE>> T select(int[] starts, int[] lengths){
		//TODO FK: this is just WIP as getLines is only used for isEmpty and AdminEnd description, but detangling requires a lot of refactoring.
		final T select = (T) doSelect(starts, lengths);

		return select;
	}



	/**
	 * Create an empty store that's only a description of the transformation.
	 */
	public ColumnStore<JAVA_TYPE> createDescription() {
		return doSelect(new int[0], new int[0]);
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


	 @JsonIgnore
	public boolean isEmpty() {
		return getLines() == 0;
	}
}
