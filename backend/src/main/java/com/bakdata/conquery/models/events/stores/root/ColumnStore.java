package com.bakdata.conquery.models.events.stores.root;

import java.util.Arrays;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;

/**
 * Base-class representing the data of a single {@link com.bakdata.conquery.models.datasets.Column} of a {@link com.bakdata.conquery.models.events.Bucket}.
 * <p>
 * Internal representation of data is completely transparent to the caller. get may only be called if has is true.
 * <p>
 * This class has subclasses per storage-type and cannot get/set itself for type-safety reasons.
 * <p>
 * Every root class must have an associated {@link MajorTypeId} and {@link com.bakdata.conquery.models.preproc.parser.Parser}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface ColumnStore {


	@JsonBackReference
	default void addParent(Bucket bucket) {

	}


	/**
	 * Helper method to select partitions of an array. Resulting array is of length sum(lengths). Incoming type T has to be of ArrayType or this will fail.
	 *
	 * @param provider method to allocate an Array of the required size.
	 */
	static <T> T selectArray(int[] starts, int[] lengths, T values, Int2ObjectFunction<T> provider) {
		int length = Arrays.stream(lengths).sum();

		final T out = provider.apply(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			System.arraycopy(values, starts[index], out, pos, lengths[index]);
			pos += lengths[index];
		}

		return out;
	}

	/**
	 * Test if the store has the event.
	 */
	boolean has(int event);

	/**
	 * Get representation of value for scripting (eg Groovy) for event.
	 */
	Object createScriptValue(int event);

	/**
	 * Calculate estimate of total bytes used by this store.
	 */
	default long estimateMemoryConsumptionBytes() {
		long bits = estimateEventBits();

		return Math.floorDiv(getLines() * bits, Byte.SIZE);
	}

	/**
	 * Number of bits required to store a single value.
	 */
	long estimateEventBits();

	/**
	 * Number of lines in this store.
	 */
	@JsonIgnore
	int getLines();

	/**
	 * Bytes required to store auxilary data.
	 */
	default long estimateTypeSizeBytes() {
		return 0;
	}

	/**
	 * Create an empty store that's only a description of the transformation.
	 */
	default ColumnStore createDescription() {
		return this.select(new int[0], new int[0]);
	}

	/**
	 * Select the partition of this store.
	 * The returning store has to accept queries up to {@code sum(lenghts)}, values may not be reordered.
	 */
	<T extends ColumnStore> T select(int[] starts, int[] lengths);

	void setNull(int event);


	@JsonIgnore
	default boolean isEmpty() {
		return getLines() == 0;
	}
}
