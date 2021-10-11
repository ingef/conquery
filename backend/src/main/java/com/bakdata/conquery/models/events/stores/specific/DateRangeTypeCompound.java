package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Stores {@link CDateRange} as pair of two {@link IntegerDateStore}s.
 */
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_COMPOUND")
@Getter
//@Setter
@ToString(of = {"minStore", "maxStore"})
public class DateRangeTypeCompound implements DateRangeStore {

	@Setter
	 private String startColumn, endColumn;

	@JsonIgnore
	@Setter
	private DateStore startStore;
	@JsonIgnore
	@Setter
	private DateStore endStore;

	@JsonBackReference
	private Bucket parent;

//	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
//	public DateRangeTypeCompound(String startColumn, String endColumn) {
//		this.startColumn = startColumn;
//		this.endColumn = endColumn;
//	}


	public void setParent(Bucket parent) {
		this.parent = parent;
		this.startStore = (DateStore) parent.getStore(startColumn);
		this.endStore = (DateStore) parent.getStore(endColumn);

		if (startStore == null) {
			throw new NullPointerException("the given minStore is null");
		}

		if (endStore == null) {
			throw new NullPointerException("the given maxStore is null");
		}
	}

	@Override
	public int getLines() {
		// they can be unaligned, if one of them is empty.
	//	return Math.max(startStore.getLines(), endStore.getLines());
		return 0;
	}

	// Reported as 0 because this store does not hold data of its own, but references its neighbouring stores.
	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public DateRangeTypeCompound select(int[] starts, int[] length) {
		DateRangeTypeCompound  compoundStore = new DateRangeTypeCompound();

		compoundStore.setStartColumn(getStartColumn());
		compoundStore.setEndColumn(getEndColumn());
		return compoundStore;
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
		return startStore.has(event) || endStore.has(event);
	}
}