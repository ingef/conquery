package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.serializer.CDateSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CDateSetSerializer;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.daterange.CDateRangeAll;
import com.bakdata.conquery.models.common.daterange.CDateRangeClosed;
import com.bakdata.conquery.models.common.daterange.CDateRangeEnding;
import com.bakdata.conquery.models.common.daterange.CDateRangeExactly;
import com.bakdata.conquery.models.common.daterange.CDateRangeStarting;
import com.bakdata.conquery.models.types.parser.specific.DateRangeParser;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import lombok.NonNull;

/**
 * Implementation of a Set for CDates (ie days since start of the UNIX-Epoch).
 *
 * This implementation stores days in a bitset. Making for extremely fast lookup and convenient operation, but at a tradeoff for memory usage. The instances should therefore be short-lived if possible. For example a span of 100years takes up approximately 4.5Kbyte. This class is much faster than the previous implementation, it would however be possible to try using RoaringBitmaps as a backend, which can handle sparse cases (however it is possible their definition of large is a few orders above ours).
 *
 */
@JsonDeserialize(using = CDateSetDeserializer.class)
@JsonSerialize(using = CDateSetSerializer.class)
public class BitMapCDateSet {

	private static final Pattern PARSE_PATTERN = Pattern.compile("(\\{|,\\s*)((\\d{4}-\\d{2}-\\d{2})?/(\\d{4}-\\d{2}-\\d{2})?)");

	/**
	 * @implNote bit 0 of negativeBits is never set as it overlaps with bit 0 of positiveBits. This is a waste of 1 bit to make code easier to read.
	 */
	private final BitSet positiveBits, negativeBits;

	private boolean openMin = false;
	private boolean openMax = false;

	private BitMapCDateSet() {
		this(new BitSet(), new BitSet());
	}

	private BitMapCDateSet(BitSet positiveBits, BitSet negativeBits) {
		this.positiveBits = positiveBits;
		this.negativeBits = negativeBits;
	}

	public static BitMapCDateSet create() {
		return new BitMapCDateSet();
	}

	/**
	 * Try to parse a CDateSet from an input string according to ISORange parsing.
	 */
	public static BitMapCDateSet parse(String value) {
		List<CDateRange> ranges = PARSE_PATTERN
										  .matcher(value)
										  .results()
										  .map(mr -> {
											  try {
												  return DateRangeParser.parseISORange(mr.group(2));
											  }
											  catch (Exception e) {
												  Throwables.throwIfUnchecked(e);
												  throw new RuntimeException(e);
											  }
										  })
										  .collect(Collectors.toList());
		return BitMapCDateSet.create(ranges);
	}

	public static BitMapCDateSet create(BitMapCDateSet orig) {
		final BitMapCDateSet set = createPreallocated(orig.negativeBits.length(), orig.positiveBits.length());

		set.positiveBits.or(orig.positiveBits);
		set.negativeBits.or(orig.negativeBits);

		set.openMax = orig.openMax;
		set.openMin = orig.openMin;

		return set;
	}

	/**
	 * Create a new CDateSet with preallocated size, avoiding costly allocations later.
	 */
	public static BitMapCDateSet createPreallocated(int min, int max) {
		return new BitMapCDateSet(new BitSet(Math.abs(min)), new BitSet(max));
	}

	public static BitMapCDateSet createAll() {
		return BitMapCDateSet.create(CDateRange.all());
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

	public boolean contains(LocalDate value) {
		return contains(CDate.ofLocalDate(value));
	}

	/**
	 * Test if the single CDate is inside the Set.
	 */
	public boolean contains(int value) {
		if (openMax && value >= getMaxRealValue()) {
			return true;
		}

		if (openMin && value <= getMinRealValue()) {
			return true;
		}

		if (value == Integer.MIN_VALUE || value == Integer.MAX_VALUE) {
			return false;
		}

		if (value >= 0 && positiveBits.get(value)) {
			return true;
		}

		return value < 0 && negativeBits.get(-value);
	}

	/**
	 * @return The highest set value if it exists, throw an exception else.
	 */
	private int getMaxRealValue() {
		int positiveMax = positiveBits.length();

		if (positiveMax != 0) {
			return positiveMax - 1;
		}


		int negativeMax = negativeBits.nextSetBit(1);

		if (negativeMax != -1) {
			return -negativeMax;
		}

		throw new IllegalStateException("Open sets have no real max value");
	}

	/**
	 * @return The lowest set value if it exists, throw an exception else.
	 */
	private int getMinRealValue() {
		int negativeMin = negativeBits.length();

		if (negativeMin != 0) {
			return -(negativeMin - 1);
		}

		int positiveMax = positiveBits.nextSetBit(0);

		if (positiveMax != -1) {
			return positiveMax;
		}

		throw new IllegalStateException("Open sets have no real min value");
	}

	/**
	 * completely reset the set, making it empty.
	 */
	public void clear() {
		openMin = false;
		openMax = false;
		positiveBits.clear();
		negativeBits.clear();
	}

	public void addAll(BitMapCDateSet other) {
		positiveBits.or(other.positiveBits);
		negativeBits.or(other.negativeBits);

		openMax = openMax || other.openMax;
		openMin = openMin || other.openMin;
	}

	public void removeAll(BitMapCDateSet other) {
		positiveBits.andNot(other.positiveBits);
		negativeBits.andNot(other.negativeBits);

		openMin = !other.openMin && openMin;
		openMax = !other.openMax && openMax;
	}

	public void addAll(Iterable<CDateRange> ranges) {
		for (CDateRange range : ranges) {
			add(range);
		}
	}

	/**
	 * Test if the range has an intersection with any of this sets subranges.
	 * <p>
	 * This means, that either of the ends is contained, or that there exists a range between the ends.
	 */
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

	public CDateRange span() {
		if (isEmpty()) {
			return null;
		}

		if (isAll()) {
			return CDateRange.all();
		}

		if (openMin) {
			return CDateRange.atMost(getMaxValue());
		}

		if (openMax) {
			return CDateRange.atLeast(getMinValue());
		}

		return CDateRange.of(getMinValue(), getMaxValue());
	}

	public boolean isEmpty() {
		return positiveBits.isEmpty() && negativeBits.isEmpty() && !openMin && !openMax;
	}

	public boolean isAll() {
		// trivial exclusion case
		if (!openMax || !openMin) {
			return false;
		}

		if (positiveBits.isEmpty() && negativeBits.isEmpty()) {
			return true;
		}

		// if min and max are open and we have a single contiguous range in the center, then we're also open!
		return higherClearBit(getMinRealValue()) - 1 == getMaxRealValue();
	}

	/**
	 * @return The highest CDate in this Set.
	 * @throws IllegalArgumentException if it is empty.
	 */
	public int getMaxValue() {
		if (isEmpty()) {
			throw new IllegalStateException("Empty range has no min/max");
		}

		if (openMax) {
			return Integer.MAX_VALUE;
		}

		if (!positiveBits.isEmpty()) {
			return positiveBits.length() - 1;
		}

		return -negativeBits.nextSetBit(0);
	}

	/**
	 * @return The lowest CDate in this Set.
	 * @throws IllegalArgumentException if it is empty.
	 */
	public int getMinValue() {
		if (isEmpty()) {
			throw new IllegalStateException("Empty range has no min/max");
		}

		if (openMin) {
			return Integer.MIN_VALUE;
		}

		if (!negativeBits.isEmpty()) {
			return -(negativeBits.length() - 1);
		}

		return positiveBits.nextSetBit(0);
	}

	/**
	 * Search for the next highest clear bit, or return {@code Integer.MIN_VALUE} if none exists.
	 */
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

	private void add(CDateRangeClosed range) {
		setRange(range.getMinValue(), range.getMaxValue() + 1);
	}

	private void add(CDateRangeExactly range) {
		final int value = range.getMinValue();

		if (value >= 0) {
			positiveBits.set(value, value + 1);
		}
		else {
			negativeBits.set(-value, -value + 1);
		}
	}

	private void add(CDateRangeAll range) {
		positiveBits.clear();
		negativeBits.clear();
		openMin = true;
		openMax = true;
	}

	private void add(CDateRangeEnding range) {
		final int value = range.getMaxValue();

		if (contains(value)) {
			openMin = true;

			// ensures that isAll always has the fastest default case, the internal state is also irrelevant at this point.
			if (isAll()) {
				positiveBits.clear();
				negativeBits.clear();
			}
			return;
		}

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
			negativeBits.set(-value, Math.max(-value + 1, Math.min(-minValue, negativeBits.size())));
		}
	}

	private void add(CDateRangeStarting range) {
		final int value = range.getMinValue();

		if (contains(value)) {
			openMax = true;

			// ensures that isAll always has the fastest default case, the internal state is also irrelevant at this point.
			if (isAll()) {
				positiveBits.clear();
				negativeBits.clear();
			}
			return;
		}

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

	public void add(CDateRange rangeToAdd) {
		if (isAll()) {
			return;
		}

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
		else if (rangeToAdd instanceof CDateRangeAll) {
			add(((CDateRangeAll) rangeToAdd));
		}
	}

	public void remove(CDateRange rangeToAdd) {
		if (isEmpty()) {
			return;
		}

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
		else if (rangeToAdd instanceof CDateRangeAll) {
			remove(((CDateRangeAll) rangeToAdd));
		}
	}

	private void remove(CDateRangeExactly range) {
		clearRange(range.getMinValue(), range.getMaxValue() + 1);
	}

	private void remove(CDateRangeClosed range) {
		clearRange(range.getMinValue(), range.getMaxValue() + 1);
	}

	private void remove(CDateRangeAll range) {
		clear();
	}

	private void remove(CDateRangeStarting range) {
		if (isEmpty()) {
			return;
		}

		if (isAll()) {
			setRange(range.getMinValue() - 1, range.getMinValue());
			openMax = false;
			return;
		}

		if (range.getMinValue() < getMaxRealValue()) {
			clearRange(range.getMinValue(), getMaxRealValue() + 1);
		}
		openMax = false;
	}

	private void remove(CDateRangeEnding range) {
		if (isEmpty()) {
			return;
		}

		if (isAll()) {
			setRange(range.getMaxValue() + 1, range.getMaxValue() + 2);
			openMin = false;
			return;
		}

		if (range.getMaxValue() > getMinRealValue()) {
			clearRange(getMinRealValue(), range.getMaxValue());
		}

		openMin = false;
	}

	/**
	 * Keep only the days present in {@code retained}, remove everything else. (Basically an AND of the sets)
	 *
	 * @param retained
	 */
	public void retainAll(BitMapCDateSet retained) {
		if (isEmpty()) {
			return;
		}

		if (isAll()) {
			negativeBits.or(retained.negativeBits);
			positiveBits.or(retained.positiveBits);

			openMin = openMin && retained.openMin;
			openMax = openMax && retained.openMax;

			return;
		}

		// expand both ways to make and-ing even possible
		if (retained.getMaxRealValue() > getMaxRealValue() && openMax) {
			setRange(getMaxRealValue(), retained.getMaxRealValue());
		}

		if (retained.getMinRealValue() < getMinValue() && openMin) {
			setRange(retained.getMinRealValue(), getMinRealValue());
		}

		negativeBits.and(retained.negativeBits);
		positiveBits.and(retained.positiveBits);

		openMin = openMin && retained.openMin;
		openMax = openMax && retained.openMax;
	}

	public void retainAll(CDateRange retained) {
		// TODO: 21.09.2020 if the need comes up, we can unroll this into a specialized method, but as it stands it would be a complicated method that has far too little usage.
		retainAll(BitMapCDateSet.create(retained));
	}

	/**
	 * Search for the next highest set bit, or return {@code Integer.MIN_VALUE} if none exists.
	 */
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

	/**
	 * Search for the next lower set bit, or return {@code Integer.MIN_VALUE} if none exists.
	 */
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

	/**
	 * Search for the next lower clean bit, or return {@code Integer.MIN_VALUE} if none exists.
	 */
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

	/**
	 * Add {@code toAdd} into this Set, but only the parts that are also in {@code mask}.
	 */
	public void maskedAdd(@NonNull CDateRange toAdd, BitMapCDateSet mask) {
		if (toAdd.isOpen()) {
			throw new IllegalArgumentException("We don't handle open ranges here. (Yet?)");
		}

		if (mask.isAll()) {
			add(toAdd);
			return;
		}

		if (mask.isEmpty()) {
			return;
		}

		// trivial but common case
		if (toAdd.isExactly() && mask.contains(toAdd.getMinValue())) {
			add(toAdd);
			return;
		}


		// from min
		{
			// is min partially contained?
			// if yes, we can insert the intersecting range
			// if not, we need to find the range that's between that and max
			final int minFromMin = mask.contains(toAdd.getMinValue()) ?
								   toAdd.getMinValue() :
								   mask.higherSetBit(toAdd.getMinValue());

			if (minFromMin != Integer.MIN_VALUE) {
				final int maxFromMin = mask.higherClearBit(minFromMin) - 1;

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
			final int maxFromMax = mask.contains(toAdd.getMaxValue()) ?
								   toAdd.getMaxValue() :
								   mask.lowerSetBit(toAdd.getMinValue());

			if (maxFromMax != Integer.MIN_VALUE) {
				final int minFromMax = mask.lowerClearBit(maxFromMax) - 1;

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

	/**
	 * The real number of days in this set.
	 *
	 * @implNote If this set is open, the number makes no sense, hence we just track the real values.
	 */
	public Long countDays() {
		return (long) (negativeBits.cardinality() + positiveBits.cardinality());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Joiner.on(", ").appendTo(sb, this.asRanges());
		sb.append('}');
		return sb.toString();
	}

	public Collection<CDateRange> asRanges() {

		if (isAll()) {
			return Collections.singletonList(CDateRange.all());
		}

		if (isEmpty()) {
			return Collections.emptyList();
		}

		final List<CDateRange> out = new ArrayList<>();

		//TODO implement this using higherSetBit etc.? Which might actually be slower since it traverses memory in reverse

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

		// Now handle special cases related to infinities and connected bitsets

		// they are indeed connected
		if (positiveBits.get(0) && negativeBits.get(1)) {
			final CDateRange centerFromLeft = out.get(center);
			final CDateRange centerFromRight = out.get(center + 1);

			// remove centerFromLeft, then replaceCenterFromRight which is now at centerFromLeft
			out.remove(center);
			out.set(center, CDateRange.of(centerFromLeft.getMinValue(), centerFromRight.getMaxValue()));
		}


		if (openMin) {
			out.set(0, CDateRange.atMost(out.get(0).getMaxValue()));
		}

		if (openMax) {
			final int last = out.size() - 1;
			out.set(last, CDateRange.atLeast(out.get(last).getMinValue()));
		}


		return out;
	}
}
