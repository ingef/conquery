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


	/**
	 * If necessary the given bucket will be set in store as parent
	 *
	 * @param bucket bucket that should be set as parent
	 * @implNote BackReference set here because Jackson does not support for fields in interfaces and abstract classes see also https://github.com/FasterXML/jackson-databind/issues/3304
	*/
	@JsonBackReference
	default void setParent(Bucket bucket) {

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
	 *
	 * @implSpec Access to getX is only defined, iff this method evaluates to true.
	 */
	boolean has(int event);

	/**
	 * Get representation of value for scripting (eg Groovy) for event.
	 */
	Object createScriptValue(int event);

	/**
	 * Calculate estimate of total bytes used by this store.
	 */
	@JsonIgnore
	default long estimateMemoryConsumptionBytes() {
		long bits = estimateEventBits();

		return Math.floorDiv(getLines() * bits, Byte.SIZE);
	}

	/**
	 * Number of bits required to store a single value.
	 */
	@JsonIgnore
	long estimateEventBits();

	/**
	 * Number of lines in this store.
	 *
	 * @implSpec Any access beyond getLines is undefined. Meaning that has and getX are not guaranteed nor are checked.
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
	@JsonIgnore
	default ColumnStore createDescription() {
		return select(new int[0], new int[0]);
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
