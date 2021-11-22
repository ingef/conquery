package com.bakdata.conquery.models.events.stores.specific;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * The {@link DateRangeTypeCompound} is almost similar to the {@link DateRangeTypeDateRange}.
 * While the {@link DateRangeTypeDateRange} stores the values of its <b>Stores</b>, the {@link DateRangeTypeCompound} does not need to store any
 * <b>Stores</b>, but only stores the references to these <b>Stores</b> and thus avoids storing these <b>Stores</b> more than once.
 * With {@link DateRangeTypeCompound}, these <b>Stores</b> are only restored after deserialization with the help of the parent {@link Bucket} and their
 * references.
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
	 *
	 * @implNote since this value is lazy loaded, do not use it directly and use the getter instead.
	 */
	@JsonIgnore
	private DateStore startStore;

	/**
	 * does not have to be serialized because will be lazy-loaded after the deserialization of DateRangeTypeCompound
	 *
	 * @implNote since this value is lazy loaded, do not use it directly and use the getter instead.
	 */
	@JsonIgnore
	private DateStore endStore;


	/**
	 * per line store if line is present.
	 */
	private BooleanStore has;


	/**
	 * Represents the bucket from where this will be saved
	 * This bucket will be set after the deserialization
	 */
	@Setter(AccessLevel.PROTECTED)
	@JsonIgnore
	private Bucket parent;

	public DateRangeTypeCompound(String startColumn, String endColumn, BooleanStore has) {
		setStartColumn(startColumn);
		setEndColumn(endColumn);
		setHas(has);
	}

	@JsonIgnore
	public DateStore getStartStore() {
		if (startStore == null) {
			setStartStore((DateStore) getParent().getStore(getStartColumn()));
		}
		return startStore;
	}

	@JsonIgnore
	public DateStore getEndStore() {

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
		return has.getLines();

	}

	/**
	 * Estimated number of bits required to store a value of type {@link DateRangeTypeCompound}.
	 *
	 * @return always 0 because this store does not hold data of its own, but references its neighbouring stores.
	 */
	@Override
	public long estimateEventBits() {
		return has.estimateEventBits();
	}

	@Override
	public DateRangeTypeCompound select(int[] starts, int[] length) {
		return new DateRangeTypeCompound(getStartColumn(), getEndColumn(), has.select(starts, length));
	}

	@Override
	public void setDateRange(int event, CDateRange raw) {
		// this is already done by the child stores, so no need to do it again
	}

	@Override
	public void setNull(int event) {
		has.setNull(event);
	}

	public void setHas(int event, boolean present) {
		has.setBoolean(event, present);
	}

	@Override
	public CDateRange getDateRange(int event) {
		int start = Integer.MIN_VALUE;
		int end = Integer.MAX_VALUE;

		final DateStore startStore = getStartStore();
		final DateStore endStore = getEndStore();

		if (startStore.has(event)) {
			start = startStore.getDate(event);
		}

		if (endStore.has(event)) {
			end = endStore.getDate(event);
		}

		return CDateRange.of(start, end);
	}

	@Override
	public boolean has(int event) {
		return has.getBoolean(event);
	}

}