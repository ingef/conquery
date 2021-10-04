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
@Setter
@ToString(of = {"minStore", "maxStore"})
public class DateRangeTypeCompound implements DateRangeStore {


	final private String min, max;

	@JsonIgnore
	private DateStore minStore;
	@JsonIgnore
	private DateStore maxStore;

	private Bucket parent;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeCompound(String min, String max) {
		this.min = min;
		this.max = max;
	}

	@JsonBackReference
	public void setParent(Bucket parent) {
		this.parent = parent;
		this.minStore = (DateStore) parent.getStore(min);
		this.maxStore = (DateStore) parent.getStore(max);
	}

	@Override
	public int getLines() {
		// they can be unaligned, if one of them is empty.
		return Math.max(minStore.getLines(), maxStore.getLines());
	}

	// returns 0 because stores are handled by another component
	@Override
	public long estimateEventBits() {
		return 0;
	}

	@Override
	public DateRangeTypeCompound select(int[] starts, int[] length) {
		return new DateRangeTypeCompound(min, max);
	}

	@Override
	public void setDateRange(int event, CDateRange raw) {
	}

	@Override
	public void setNull(int event) {
	}

	@Override
	public CDateRange getDateRange(int event) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (minStore.has(event)) {
			min = minStore.getDate(event);
		}

		if (maxStore.has(event)) {
			max = maxStore.getDate(event);
		}

		return CDateRange.of(min, max);
	}

	@Override
	public boolean has(int event) {
		return minStore.has(event) || maxStore.has(event);
	}
}