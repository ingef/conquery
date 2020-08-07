package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.base.Joiner;


// TODO: 07.08.2020 FK: Use roaring Bitmaps instead
public class BitMapCDateSet implements ICDateSet {

	public static BitMapCDateSet create() {
		return new BitMapCDateSet();
	}

	private boolean openMin = false;
	private boolean openMax = false;

	private final BitSet positiveBits = new BitSet();
	private final BitSet negativeBits = new BitSet();

	public Collection<CDateRange> asRanges() {
		final Collection<CDateRange> out = new ArrayList<>();

		if(openMin){
			// https://www.javadoc.io/static/org.roaringbitmap/RoaringBitmap/0.9.1/org/roaringbitmap/RoaringBitmap.html#addOffset(org.roaringbitmap.RoaringBitmap,long)
		}

		if (!positiveBits.isEmpty()) {
			int start = positiveBits.nextSetBit(0);


			while (start != -1) {
				int end = positiveBits.nextClearBit(start);
				out.add(CDateRange.of(start, end - 1));

				start = positiveBits.nextSetBit(end);
			}
		}

		if (!negativeBits.isEmpty()) {
			int start = negativeBits.nextSetBit(0);

			while (start != -1) {
				int end = negativeBits.nextClearBit(start);

				out.add(CDateRange.of(-start, -(end - 1)));

				start = negativeBits.nextSetBit(end);
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

	@Override
	public boolean enclosesAll(ICDateSet other) {
		return false;
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


	public void add(CDateRange range) {
		if (range.isExactly()) {
			final int value = range.getMinValue();

			if (value >= 0) {
				positiveBits.set(value, value + 1);
			}
			else {
				negativeBits.set(-value, -value - 1);
			}
			return;
		}

		if(!range.isOpen()){
			positiveBits.set(Math.max(0, range.getMinValue()), Math.max(0, range.getMaxValue()) + 1);
			negativeBits.set(-Math.min(0, range.getMinValue()), -Math.min(0, range.getMaxValue() + 1));
			return;
		}

		if(range.isAtMost()) {
			openMin = true;
			final int value = range.getMaxValue();
			if(value >= 0 && value < getMaxValue()){
				positiveBits.set(value,getMaxValue());
			}
			else if (value < 0 && value > getMinValue()) {
				negativeBits.set(-value, -getMinValue());
			}
			return;
		}

		if(range.isAtLeast()) {
			openMax = true;
			final int value = range.getMinValue();
			if(value >= 0 && value < getMaxValue()){
				positiveBits.set(value,getMaxValue());
			}
			else if (value < 0 && value > getMinValue()) {
				negativeBits.set(-value, -getMinValue());
			}
			return;
		}

		//TODO infinities by setting everything above positiveBits.size() and setting

	}


	public void remove(CDateRange range) {
		positiveBits.clear(Math.max(0, range.getMinValue()), Math.max(0, range.getMaxValue()) + 1);
		negativeBits.clear(-Math.min(0, range.getMinValue()), -Math.min(0, range.getMaxValue() + 1));
	}


	public boolean isAll() {
		return openMax && openMin;
	}


	public void retainAll(ICDateSet retained) {
		if (retained instanceof BitMapCDateSet) {
			negativeBits.and(((BitMapCDateSet) retained).negativeBits);
			positiveBits.and(((BitMapCDateSet) retained).positiveBits);
		}
	}


	public void retainAll(CDateRange retained) {
		final BitSet bits = new BitSet(Math.max(Math.abs(retained.getMinValue()), Math.abs(retained.getMaxValue())));
		bits.set(Math.max(0, retained.getMinValue()), Math.max(0, retained.getMaxValue()) + 1);

		positiveBits.and(bits);

		bits.clear();

		bits.set(-Math.min(0, retained.getMaxValue()), -Math.min(0, retained.getMinValue()) + 1);
		negativeBits.and(bits);
	}


	public Long countDays() {
		return (long) (negativeBits.cardinality() + positiveBits.cardinality());
	}


	public int getMinValue() {
		if (openMin) {
			return Integer.MIN_VALUE;
		}

		if (!negativeBits.isEmpty()) {
			return -(negativeBits.length() - 1);
		}

		return positiveBits.nextSetBit(0);
	}


	public int getMaxValue() {
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
