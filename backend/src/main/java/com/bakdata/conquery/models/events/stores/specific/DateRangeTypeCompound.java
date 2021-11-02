package com.bakdata.conquery.models.events.stores.specific;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Stores {@link CDateRange} by storing only the references of the start and end of the {@link CDateRange}.
 * After the deserialisation, stores are instantiated as {@link DateStore}s using the references to the columns.
 */
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_COMPOUND")
@Getter
@Setter
@ToString(of = {"startColumn", "endColumn"})
public class DateRangeTypeCompound implements DateRangeStore {


	@NotNull
	@NotEmpty
	private String startColumn, endColumn;

	/**
	  * does not have to be serialized because will be lazy-loaded after the deserialization of DateRangeTypeCompound
	  * @implNote since this value is lazy loaded, do not use it directly and use the getter instead.
	  */
	@JsonIgnore
	private DateStore startStore;

	// does not have to be serialized because will be lazy-loaded after the deserialization of DateRangeTypeCompound
	@JsonIgnore
	private DateStore endStore;


	/**
	 * Represents the bucket from where this will be saved
	 * This bucket will be set after the deserialization
	 */
	@Setter(AccessLevel.PROTECTED)
	@JsonIgnore
	private Bucket parent;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeCompound(String startColumn, String endColumn) {
		setStartColumn(startColumn);
		setEndColumn(endColumn);
	}

	@JsonIgnore
	public DateStore getStartStore() {
		if (getParent() == null) {
			return null;
		}
		if (startStore == null) {
			setStartStore((DateStore) getParent().getStore(getStartColumn()));
		}
		return startStore;
	}

	@JsonIgnore
	public DateStore getEndStore() {
		if (getParent() == null) {
			return null;
		}
		if (endStore == null) {
			setEndStore((DateStore) getParent().getStore(getEndColumn()));
		}
		return endStore;
	}

	@Override
	public void setParent(@NonNull Bucket bucket) {
		parent = bucket;
	}

	@Override
	public int getLines() {
		if (getStartStore() == null && getEndStore() == null) {
			return 0;
		}
		if (getStartStore() == null || getEndStore() == null) {
			throw new IllegalStateException(String.format("DateRangeTypeCompound[%s] has one null store.", this));
		}
		// they can be unaligned, if one of them is empty.
		return Math.max(getStartStore().getLines(), getEndStore().getLines());

	}

	// Reported as 0 because this store does not hold data of its own, but references its neighbouring stores.
	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public DateRangeTypeCompound select(int[] starts, int[] length) {
		return new DateRangeTypeCompound(getStartColumn(), getEndColumn());
	}

	@Override
	public void setDateRange(int event, CDateRange raw) {
		// this has already done by the child stores, so no need to do it again
	}

	@Override
	public void setNull(int event) {
		// this has already been done by the child stores, so no need to do it again
	}

	@Override
	public CDateRange getDateRange(int event) {
		int start = Integer.MIN_VALUE;
		int end = Integer.MAX_VALUE;

		if (getStartStore().has(event)) {
			start = getStartStore().getDate(event);
		}

		if (getEndStore().has(event)) {
			end = getEndStore().getDate(event);
		}

		return CDateRange.of(start, end);
	}

	@Override
	public boolean has(int event) {
		return getStartStore().has(event) || getEndStore().has(event);
	}
}