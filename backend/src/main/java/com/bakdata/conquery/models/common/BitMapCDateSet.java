package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.daterange.CDateRangeClosed;
import com.bakdata.conquery.models.common.daterange.CDateRangeEnding;
import com.bakdata.conquery.models.common.daterange.CDateRangeExactly;
import com.bakdata.conquery.models.common.daterange.CDateRangeOpen;
import com.bakdata.conquery.models.common.daterange.CDateRangeStarting;
import com.google.common.base.Joiner;


public class BitMapCDateSet implements ICDateSet {

	public static BitMapCDateSet create() {
		return new BitMapCDateSet();
	}

	public static BitMapCDateSet create(CDateRange... dates) {
		final BitMapCDateSet out = new BitMapCDateSet();

		for (CDateRange date : dates) {
			out.add(date);
		}

		return out;
	}

	private boolean openMin = false;
	private boolean openMax = false;

	private final BitSet positiveBits = new BitSet();
	/**
	 * @implNote bit 0 is never set as it overlaps with bit 0 of positiveBits. This is a waste of 1 bit to make code easier to read.
	 */
	private final BitSet negativeBits = new BitSet();


	public Collection<CDateRange> asRanges() {
		final List<CDateRange> out = new ArrayList<>();

		if (!negativeBits.isEmpty()) {
			int start = negativeBits.nextSetBit(0);

			while (start != -1) {
				int end = negativeBits.nextClearBit(start);

				out.add(CDateRange.of(-(end - 1), -start));

				start = negativeBits.nextSetBit(end);
			}
		}

		Collections.reverse(out);

		// this is the Range in the middle, we might need this if negative and positive bits are connected.
		int center = out.size() - 1;

		if (!positiveBits.isEmpty()) {
			int start = positiveBits.nextSetBit(0);

			while (start != -1) {
				int end = positiveBits.nextClearBit(start);
				out.add(CDateRange.of(start, end - 1));

				start = positiveBits.nextSetBit(end);
			}
		}

		// they are indeed connected
		if (positiveBits.get(0) && negativeBits.get(1)) {
			final CDateRange centerFromLeft = out.get(center);
			final CDateRange centerFromRight = out.get(center + 1);

			// remove centerFromLeft, then replaceCenterFromRight which is now at centerFromLeft
			out.remove(center);
			out.set(center, CDateRange.of(centerFromLeft.getMinValue(), centerFromRight.getMaxValue()));
		}

		if (openMin && openMax && out.isEmpty()) {
			out.add(CDateRange.all());
		}
		else if (openMin && openMax && out.size() == 1) {
			final CDateRange middle = out.get(0);

			out.clear();
			out.add(CDateRange.atMost(middle.getMinValue()));
			out.add(CDateRange.atLeast(middle.getMaxValue()));
		}
		else {
			if (openMin) {
				out.set(0, CDateRange.atMost(out.get(0).getMaxValue()));
			}
			if (openMax) {
				final int last = out.size() - 1;

				out.set(last, CDateRange.atLeast(out.get(last).getMinValue()));
			}
		}

		return out;
	}

	public CDateRange rangeContaining(int value) {
		if (!contains(value)) {
			return null;
		}

		return CDateRange.of(positiveBits.previousClearBit(value) + 1, positiveBits.nextClearBit(value) - 1);
	}


	public boolean contains(LocalDate value) {
		return contains(CDate.ofLocalDate(value));
	}


	public boolean contains(int value) {
		return positiveBits.get(value);
	}


	public boolean isEmpty() {
		return positiveBits.isEmpty();
	}


	public void clear() {
		positiveBits.clear();
	}


	public void addAll(ICDateSet other) {
		if (other instanceof BitMapCDateSet) {
			positiveBits.or(((BitMapCDateSet) other).positiveBits);
			negativeBits.or(((BitMapCDateSet) other).negativeBits);
		}
	}


	public void removeAll(ICDateSet other) {
		if (other instanceof BitMapCDateSet) {
			positiveBits.andNot(((BitMapCDateSet) other).positiveBits);
			negativeBits.andNot(((BitMapCDateSet) other).negativeBits);
		}
	}


	public boolean enclosesAll(Iterable<CDateRange> other) {
		for (CDateRange cDateRange : other) {
			if (!encloses(cDateRange)) {
				return false;
			}
		}

		return true;
	}


	public void addAll(Iterable<CDateRange> ranges) {
		for (CDateRange range : ranges) {
			add(range);
		}
	}


	public void removeAll(Iterable<CDateRange> ranges) {
		for (CDateRange range : ranges) {
			remove(range);
		}
	}

	public boolean intersects(CDateRange range) {
		return !positiveBits.get(range.getMinValue(), range.getMaxValue()).isEmpty();
	}


	public boolean encloses(CDateRange range) {
		final BitSet hits = positiveBits.get(range.getMinValue(), range.getMaxValue());
		return hits.size() - 1 == hits.cardinality();
	}


	public CDateRange span() {
		return CDateRange.of(getMinValue(), getMaxValue());
	}


	private void setRange(int from, int to) {
		positiveBits.set(Math.max(0, from), Math.max(0, to));

		if (from < 0) {
			from = -from;
			to = Math.max(1, -to);

			negativeBits.set(to, from + 1);
		}
	}

	private void clearRange(int from, int to) {

		// if that range is beyond our highest bit but inside the range, we need to extend it first
		if (openMax && to >= positiveBits.length()) {
			positiveBits.set(positiveBits.length(), to + 1);
		}

		if (openMin && from <= 0 && -from >= negativeBits.length()) {
			negativeBits.set(Math.max(1, negativeBits.length()), -from + 2);
		}

		positiveBits.clear(Math.max(0, from), Math.max(0, to));

		if (from < 0) {
			from = -from;
			to = Math.max(1, -to);

			negativeBits.clear(to, from + 1);
		}
	}

	public void add(CDateRangeClosed range) {
		setRange(range.getMinValue(), range.getMaxValue() + 1);
	}


	public void add(CDateRangeExactly range) {
		final int value = range.getMinValue();

		if (value >= 0) {
			positiveBits.set(value, value + 1);
		}
		else {
			negativeBits.set(-value, -value + 1);
		}
	}

	public void add(CDateRangeOpen range) {
		positiveBits.clear();
		negativeBits.clear();
		openMin = true;
		openMax = true;
	}

	public void add(CDateRangeEnding range) {

		final int value = range.getMaxValue();
		openMin = true;

		final int maxValue = getMaxValue();
		final int minValue = getMinValue();


		if (value >= 0) {
			positiveBits.set(value);
		}
		else {
			negativeBits.set(-value);
		}

		if (value >= 0 && value < maxValue) {
			positiveBits.set(value, maxValue);
		}
		else if (value < 0 && value > minValue) {
			negativeBits.set(-value, -minValue);
		}
	}

	public void add(CDateRangeStarting range) {

		final int value = range.getMinValue();
		openMax = true;

		final int maxValue = getMaxValue();
		final int minValue = getMinValue();


		if (value >= 0) {
			positiveBits.set(value);
		}
		else {
			negativeBits.set(-value);
		}

		if (value >= 0 && value < maxValue) {
			positiveBits.set(value, maxValue);
		}
		else if (value < 0 && value > minValue) {
			negativeBits.set(-value, -minValue);
		}
	}

	@Override
	public void add(CDateRange rangeToAdd) {
		if (rangeToAdd instanceof CDateRangeClosed) {
			add(((CDateRangeClosed) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeExactly) {
			add(((CDateRangeExactly) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeStarting) {
			add(((CDateRangeStarting) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeEnding) {
			add(((CDateRangeEnding) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeOpen) {
			add(((CDateRangeOpen) rangeToAdd));
		}
	}

	@Override
	public void remove(CDateRange rangeToAdd) {
		if (rangeToAdd instanceof CDateRangeClosed) {
			remove(((CDateRangeClosed) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeExactly) {
			remove(((CDateRangeExactly) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeStarting) {
			remove(((CDateRangeStarting) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeEnding) {
			remove(((CDateRangeEnding) rangeToAdd));
		}
		else if (rangeToAdd instanceof CDateRangeOpen) {
			remove(((CDateRangeOpen) rangeToAdd));
		}
	}

	public void remove(CDateRangeExactly range) {
		clearRange(range.getMinValue(), range.getMaxValue() + 1);
	}

	public void remove(CDateRangeClosed range) {
		clearRange(range.getMinValue(), range.getMaxValue() + 1);
	}

	public void remove(CDateRangeOpen range) {
		positiveBits.clear();
		negativeBits.clear();
		openMax = false;
		openMin = false;
	}

	public void remove(CDateRangeStarting range) {
		clearRange(range.getMinValue(), getMaxValue() + 1);
		openMax = false;
	}

	public void remove(CDateRangeEnding range) {
		clearRange(getMinValue(), range.getMaxValue());
		openMin = false;
	}

	public boolean isAll() {
		return openMax && openMin && positiveBits.isEmpty() && negativeBits.isEmpty();
	}


	public void retainAll(ICDateSet retained) {
		if (retained instanceof BitMapCDateSet) {
			negativeBits.and(((BitMapCDateSet) retained).negativeBits);
			positiveBits.and(((BitMapCDateSet) retained).positiveBits);
		}
	}


	public void retainAll(CDateRange retained) {
		positiveBits.clear(0, Math.max(0, retained.getMinValue()));
		positiveBits.clear(Math.max(0, retained.getMaxValue()), positiveBits.length());

		negativeBits.clear(0, -Math.max(-1, retained.getMinValue()));
		negativeBits.clear(-Math.max(-1, retained.getMaxValue()), positiveBits.length());
	}


	public Long countDays() {
		return (long) (negativeBits.cardinality() + positiveBits.cardinality());
	}


	public int getMinValue() {
		if (isEmpty()) {
			return Integer.MAX_VALUE;
		}

		if (openMin) {
			return Integer.MIN_VALUE;
		}

		if (!negativeBits.isEmpty()) {
			return -(negativeBits.length() - 1);
		}

		return positiveBits.nextSetBit(0);
	}


	public int getMaxValue() {
		if (isEmpty()) {
			return Integer.MIN_VALUE;
		}

		if (openMax) {
			return Integer.MAX_VALUE;
		}

		if (!positiveBits.isEmpty()) {
			return positiveBits.length() - 1;
		}

		return -negativeBits.nextSetBit(0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Joiner.on(", ").appendTo(sb, this.asRanges());
		sb.append('}');
		return sb.toString();
	}
}
