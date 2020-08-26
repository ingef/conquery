package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.daterange.CDateRangeClosed;
import com.bakdata.conquery.models.common.daterange.CDateRangeEnding;
import com.bakdata.conquery.models.common.daterange.CDateRangeExactly;
import com.bakdata.conquery.models.common.daterange.CDateRangeOpen;
import com.bakdata.conquery.models.common.daterange.CDateRangeStarting;
import com.bakdata.conquery.models.types.parser.specific.DateRangeParser;
import com.google.common.base.Joiner;
import lombok.NonNull;


public class BitMapCDateSet implements ICDateSet {

	private BitMapCDateSet(BitSet positiveBits, BitSet negativeBits) {
		this.positiveBits = positiveBits;
		this.negativeBits = negativeBits;
	}

	private BitMapCDateSet() {
		this(new BitSet(), new BitSet());
	}

	public static BitMapCDateSet create() {
		return new BitMapCDateSet();
	}

	public static BitMapCDateSet createFull() {
		final BitMapCDateSet set = new BitMapCDateSet();
		set.add(CDateRange.all());
		return set;
	}

	private static final Pattern PARSE_PATTERN = Pattern.compile("(\\{|,\\s*)((\\d{4}-\\d{2}-\\d{2})?/(\\d{4}-\\d{2}-\\d{2})?)");

	public static BitMapCDateSet parse(String value) {
		List<CDateRange> ranges = PARSE_PATTERN
										  .matcher(value)
										  .results()
										  .map(mr -> {
											  try {
												  return DateRangeParser.parseISORange(mr.group(2));
											  }
											  catch (Exception e) {
												  throw new RuntimeException(e);
											  }
										  })
										  .collect(Collectors.toList());
		return BitMapCDateSet.create(ranges);
	}

	public static BitMapCDateSet create(BitMapCDateSet orig) {
		final BitMapCDateSet set = create(orig.negativeBits.length(), orig.positiveBits.length());

		set.positiveBits.or(orig.positiveBits);
		set.negativeBits.or(orig.negativeBits);

		set.openMax = orig.openMax;
		set.openMin = orig.openMin;

		return set;
	}

	public static BitMapCDateSet createAll() {
		final BitMapCDateSet out = new BitMapCDateSet();
		out.openMin = true;
		out.openMax = true;
		return out;
	}

	public static BitMapCDateSet create(int min, int max) {
		return new BitMapCDateSet(new BitSet(Math.abs(min)), new BitSet(max));
	}

	public static BitMapCDateSet create(CDateRange... dates) {
		final BitMapCDateSet out = new BitMapCDateSet();

		for (CDateRange date : dates) {
			out.add(date);
		}

		return out;
	}

	public static BitMapCDateSet create(Iterable<CDateRange> dates) {
		final BitMapCDateSet out = new BitMapCDateSet();

		for (CDateRange date : dates) {
			out.add(date);
		}

		return out;
	}

	private boolean openMin = false;
	private boolean openMax = false;

	private final BitSet positiveBits;
	/**
	 * @implNote bit 0 is never set as it overlaps with bit 0 of positiveBits. This is a waste of 1 bit to make code easier to read.
	 */
	private final BitSet negativeBits;


	public Collection<CDateRange> asRanges() {
		final List<CDateRange> out = new ArrayList<>();

		//TODO implement this using higherSetBit etc.

		// Iterate negative ranges first
		if (!negativeBits.isEmpty()) {
			int start = negativeBits.nextSetBit(0);

			while (start != -1) {
				int end = negativeBits.nextClearBit(start);

				out.add(CDateRange.of(-(end - 1), -start));

				start = negativeBits.nextSetBit(end);
			}
		}

		// Then reverse their order as they are starting at zero
		Collections.reverse(out);

		// this is the Range in the middle, we might need this if negative and positive bits are connected.
		int center = out.size() - 1;

		// Then iterate positive ranges
		if (!positiveBits.isEmpty()) {
			int start = positiveBits.nextSetBit(0);

			while (start != -1) {
				int end = positiveBits.nextClearBit(start);
				out.add(CDateRange.of(start, end - 1));

				start = positiveBits.nextSetBit(end);
			}
		}

		// Now handle special cases related to infinities and connectedness of the bitsets

		// they are indeed connected
		if (positiveBits.get(0) && negativeBits.get(1)) {
			final CDateRange centerFromLeft = out.get(center);
			final CDateRange centerFromRight = out.get(center + 1);

			// remove centerFromLeft, then replaceCenterFromRight which is now at centerFromLeft
			out.remove(center);
			out.set(center, CDateRange.of(centerFromLeft.getMinValue(), centerFromRight.getMaxValue()));
		}


		if (isAll()) {
			out.add(CDateRange.all());
		}
		else if (openMin && openMax && out.size() == 1) {
			final CDateRange middle = out.get(0);
			//todo I think  this might actually be an invalid range
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

		// TODO: 13.08.2020 fk: still missing infinities

		if (value < 0) {
			final int left = -negativeBits.nextClearBit(-value) + 1;

			int right = negativeBits.previousClearBit(-value);

			if (right != -1) {
				return CDateRange.of(left, -right + 1);
			}

			// TODO: 18.08.2020 fk  this can actually also be invalid!

			return CDateRange.of(left, positiveBits.nextClearBit(value) - 1);
		}

		// TODO: 13.08.2020 this is missing negative bits
		return CDateRange.of(positiveBits.previousClearBit(value) + 1, positiveBits.nextClearBit(value) - 1);
	}


	public boolean contains(LocalDate value) {
		return contains(CDate.ofLocalDate(value));
	}


	// TODO: 19.08.2020 these waste cycles if the value lengths are known
	public int getMaxRealValue() {
		return Math.max(-negativeBits.nextSetBit(1), positiveBits.length());
	}

	public int getMinRealValue() {
		return Math.min(positiveBits.nextSetBit(0), -(negativeBits.length() - 1));
	}

	public boolean contains(int value) {
		if (value >= 0 && positiveBits.get(value)) {
			return true;
		}

		if (value < 0 && negativeBits.get(-value)) {
			return true;
		}

		if (openMax && value >= getMaxRealValue()) {
			return true;
		}

		if (openMin && value <= getMinRealValue()) {
			return true;
		}

		return false;
	}


	public boolean isEmpty() {
		return positiveBits.isEmpty() && negativeBits.isEmpty() && !openMin && !openMax;
	}


	public void clear() {
		positiveBits.clear();
		negativeBits.clear();
	}


	public void addAll(ICDateSet other) {
		if (other instanceof BitMapCDateSet) {
			positiveBits.or(((BitMapCDateSet) other).positiveBits);
			negativeBits.or(((BitMapCDateSet) other).negativeBits);
		}
		else if (other instanceof CDateSet) {
			addAll(other.asRanges());
		}
	}


	public void removeAll(ICDateSet other) {
		if (other instanceof BitMapCDateSet) {
			positiveBits.andNot(((BitMapCDateSet) other).positiveBits);
			negativeBits.andNot(((BitMapCDateSet) other).negativeBits);
		}
		else if (other instanceof CDateSet) {
			addAll(other.asRanges());
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
		// trivial case
		if (contains(range.getMinValue()) || contains(range.getMaxValue())) {
			return true;
		}

		if (range.getMinValue() < 0) {
			int intersection = negativeBits.previousSetBit(-range.getMinValue());

			if (intersection != -1) {
				return -intersection <= range.getMaxValue();
			}

			intersection = positiveBits.nextSetBit(0);

			return intersection != -1 && intersection <= range.getMaxValue();
		}

		int intersection = positiveBits.nextSetBit(range.getMinValue());

		return intersection != -1 && intersection <= range.getMaxValue();
	}


	public boolean encloses(CDateRange range) {
		final CDateRange rangeContaining = rangeContaining(range.getMinValue());
		// todo inline rangeContaining to not waste that range in the call
		return rangeContaining != null && rangeContaining.contains(range.getMaxValue());
	}


	public CDateRange span() {
		if (isEmpty()) {
			return null;
		}

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
			// TODO: 13.08.2020 this does not look right?
			negativeBits.set(-value, Math.max(-value + 1, Math.min(-minValue, negativeBits.size())));
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
		if (range.getMinValue() < getMaxRealValue()) {
			clearRange(range.getMinValue(), getMaxRealValue() + 1);
		}
		openMax = false;
	}

	public void remove(CDateRangeEnding range) {
		if (range.getMaxValue() > getMinRealValue()) {
			clearRange(getMinRealValue(), range.getMaxValue());
		}

		openMin = false;
	}

	public boolean isAll() {
		return openMax && openMin && positiveBits.isEmpty() && negativeBits.isEmpty();
	}


	public void retainAll(ICDateSet retained) {
		if (retained instanceof BitMapCDateSet) {
			final BitMapCDateSet dateSet = (BitMapCDateSet) retained;

			// expand both ways to make anding even possible
			if(dateSet.getMaxRealValue() > getMaxRealValue() && openMax) {
				setRange(getMaxRealValue(), dateSet.getMaxRealValue());
			}

			if(dateSet.getMinRealValue() < getMinValue() && openMin) {
				setRange(dateSet.getMinRealValue(), getMinRealValue());
			}

			negativeBits.and(dateSet.negativeBits);
			positiveBits.and(dateSet.positiveBits);

			openMin = dateSet.openMin;
			openMax = dateSet.openMax;
		}
	}


	public void retainAll(CDateRange retained) {
		remove(CDateRange.atMost(retained.getMinValue()));
		remove(CDateRange.atLeast(retained.getMaxValue()));
	}


	protected int higherSetBit(int value) {
		if (value < 0) {
			int out = negativeBits.previousSetBit(-value);

			if (out != -1) {
				return -out;
			}

			out = positiveBits.nextSetBit(0);

			if (out == -1) {
				return Integer.MIN_VALUE;
			}

			return out;
		}

		final int out = positiveBits.nextSetBit(value);

		if (out == -1) {
			return Integer.MIN_VALUE;
		}

		return out;
	}

	protected int higherClearBit(int value) {
		if (value < 0) {
			int out = negativeBits.previousClearBit(-value);

			if (out != -1) {
				return -out;
			}

			out = positiveBits.nextClearBit(0);

			if (out == -1) {
				return Integer.MIN_VALUE;
			}

			return out;
		}

		int out = positiveBits.nextClearBit(value);

		if (out == -1) {
			return Integer.MIN_VALUE;
		}
		return out;
	}

	protected int lowerSetBit(int value) {
		if (value >= 0) {
			int out = positiveBits.previousSetBit(value);

			if (out != -1) {
				return out;
			}

			out = negativeBits.nextSetBit(1);

			if (out != -1) {
				return -out;
			}

			return Integer.MIN_VALUE;
		}

		int out = negativeBits.nextSetBit(1);

		if (out != -1) {
			return -out;
		}

		return Integer.MIN_VALUE;
	}

	protected int lowerClearBit(int value) {
		if (value >= 0) {
			int out = positiveBits.previousClearBit(value);

			if (out != -1) {
				return out;
			}

			out = negativeBits.nextClearBit(1);

			if (out != -1) {
				return -out;
			}

			return Integer.MIN_VALUE;
		}

		int out = negativeBits.nextClearBit(1);

		if (out != -1) {
			return -out;
		}

		return Integer.MIN_VALUE;
	}


	public void maskedAdd(@NonNull CDateRange toAdd, @NonNull ICDateSet mask) {
		if (toAdd.isOpen()) {
			throw new IllegalArgumentException("We don't handle open ranges here. (Yet?)");
		}

		if (mask instanceof CDateSet) {
			return;
		}

		if (mask.isAll()) {
			add(toAdd);
			return;
		}

		if (mask.isEmpty()) {
			return;
		}

		// trivial but common case
		if (toAdd instanceof CDateRangeExactly && mask.contains(toAdd.getMinValue())) {
			add(toAdd);
			return;
		}


		BitMapCDateSet _mask = (BitMapCDateSet) mask;


		// from min
		{
			// is min partially contained?
			// if yes, we can insert the intersecting range
			// if not, we need to find the range that's between that and max
			final int minFromMin = _mask.contains(toAdd.getMinValue()) ?
								   toAdd.getMinValue() :
								   _mask.higherSetBit(toAdd.getMinValue());

			if (minFromMin != Integer.MIN_VALUE) {
				final int maxFromMin = _mask.higherClearBit(minFromMin) - 1;

				if (maxFromMin != Integer.MIN_VALUE) {
					if (maxFromMin < toAdd.getMaxValue()) {
						add(CDateRange.of(minFromMin, maxFromMin));
					}
					// it's fully contained
					else {
						add(CDateRange.of(minFromMin, toAdd.getMaxValue()));
						return;
					}
				}
			}
		}

		// from max
		{
			final int maxFromMax = _mask.contains(toAdd.getMaxValue()) ?
								   toAdd.getMaxValue() :
								   _mask.lowerSetBit(toAdd.getMinValue());

			if (maxFromMax != Integer.MIN_VALUE) {
				final int minFromMax = _mask.lowerClearBit(maxFromMax) - 1;

				if (minFromMax != Integer.MIN_VALUE) {
					if (minFromMax > toAdd.getMinValue()) {
						add(CDateRange.of(minFromMax, maxFromMax));
					}
					// it's fully contained
					else {
						add(CDateRange.of(toAdd.getMinValue(), maxFromMax));
						return;
					}
				}
			}
		}
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
