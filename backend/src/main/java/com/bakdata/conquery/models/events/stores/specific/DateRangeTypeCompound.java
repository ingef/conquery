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
import lombok.Setter;
import lombok.ToString;

/**
 * Stores {@link CDateRange} as pair of two {@link IntegerDateStore}s.
 */
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_COMPOUND")
@Getter
@Setter
@ToString(of = {"minStore", "maxStore"})
public class DateRangeTypeCompound implements DateRangeStore {


	@NotNull
	@NotEmpty
	private String startColumn, endColumn;

	@JsonIgnore
	private DateStore startStore;

	@JsonIgnore
	private DateStore endStore;


	@Setter(AccessLevel.PROTECTED)
	@JsonIgnore
	private Bucket parent;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeCompound(String startColumn, String endColumn) {
		this.startColumn = startColumn;
		this.endColumn = endColumn;
	}

	@JsonIgnore
	public DateStore getStartStore() {
		if (startStore == null) {
			this.startStore = (DateStore) parent.getStore(startColumn);
		}
		return startStore;
	}

	@JsonIgnore
	public DateStore getEndStore() {
		if (endStore == null) {
			this.endStore = (DateStore) parent.getStore(endColumn);
		}
		return endStore;
	}

	@Override
	public void addParent(Bucket bucket) {
		this.parent = bucket;
	}

	@Override
	public int getLines() {
		// they can be unaligned, if one of them is empty.
		return Math.max(startStore.getLines(), endStore.getLines());

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