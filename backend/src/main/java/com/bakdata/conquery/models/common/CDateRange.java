package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@NoArgsConstructor
@Wither
@EqualsAndHashCode
public class CDateRange implements IRange<LocalDate, CDateRange> {

	private int min = Integer.MIN_VALUE;
	private int max = Integer.MAX_VALUE;

	public CDateRange(Range<Integer> orig) {
		this(orig.getMin(), orig.getMax());
	}

	public CDateRange(int min, int max) {
		this.min = min;
		this.max = max;

		if (min > max) {
			throw new IllegalArgumentException(String.format("Min(%s) is not less than max(%s)", CDate.toLocalDate(min), CDate.toLocalDate(max)));
		}
	}

	public static CDateRange of(Range<LocalDate> orig) {
		return of(orig.getMin(), orig.getMax());
	}

	public static CDateRange exactly(LocalDate value) {
		return new CDateRange(CDate.ofLocalDate(value), CDate.ofLocalDate(value));
	}

	public static CDateRange atLeast(LocalDate value) {
		return new CDateRange(CDate.ofLocalDate(value), Integer.MAX_VALUE);
	}

	public static CDateRange atMost(LocalDate value) {
		return new CDateRange(Integer.MIN_VALUE, CDate.ofLocalDate(value));
	}

	@JsonCreator
	public static CDateRange of(LocalDate min, LocalDate max) {
		return new CDateRange(CDate.ofLocalDate(min), CDate.ofLocalDate(max));
	}

	public static CDateRange all() {
		return new CDateRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public LocalDate getMax() {
		return CDate.toLocalDate(getMaxValue());
	}

	@JsonIgnore
	public int getMaxValue() {
		return max;
	}

	@JsonIgnore
	public int getMinValue() {
		return min;
	}

	@Override
	public LocalDate getMin() {
		return CDate.toLocalDate(getMinValue());
	}

	@JsonCreator
	public CDateRange fromArray(int[] values) {
		if (values.length != 2) {
			throw new IllegalArgumentException("Array must be exactly of size 2");
		}

		return new CDateRange(values[0], values[1]);
	}

	public CDateRange intersection(CDateRange other) {
		if (!intersects(other)) {
			throw new IllegalArgumentException("Ranges do not intersect.");
		}

		return new CDateRange(Math.max(min, other.min), Math.min(max, other.max));
	}

	@JsonValue
	public int[] asArray() {
		return new int[]{min, max};
	}

	public Range<Integer> asIntegerRange() {
		return Range.of(min, max);
	}

	@Override
	public boolean contains(LocalDate value) {
		return contains(CDate.ofLocalDate(value));
	}

	@Override
	public boolean contains(CDateRange other) {
		return contains(other.getMinValue()) && contains(other.getMaxValue());
	}

	public boolean contains(int rep) {
		return rep >= min && rep <= max;
	}

	@Override
	public CDateRange span(CDateRange other) {
		return new CDateRange(Math.min(getMinValue(), other.getMinValue()), Math.max(getMaxValue(), other.getMaxValue()));
	}

	@Override
	public boolean isOpen() {
		return getMinValue() == Integer.MIN_VALUE || getMaxValue() == Integer.MAX_VALUE;
	}

	@Override
	public boolean isAll() {
		return getMinValue() == Integer.MIN_VALUE && getMaxValue() == Integer.MAX_VALUE;
	}

	@Override
	public boolean isAtMost() {
		return getMinValue() == Integer.MIN_VALUE && getMaxValue() != Integer.MAX_VALUE;
	}

	@Override
	public boolean isAtLeast() {
		return getMinValue() != Integer.MIN_VALUE && getMaxValue() == Integer.MAX_VALUE;
	}

	@Override
	public boolean isExactly() {
		return getMinValue() == getMaxValue();
	}

	@JsonIgnore
	public long getDurationInDays() {
		return getDuration(ChronoUnit.DAYS);
	}

	@JsonIgnore
	public long getNumberOfDays() {
		return getDurationInDays() + 1;
	}

	@JsonIgnore
	public long getDuration(ChronoUnit unit) {
		return unit.between(getMin(), getMax());
	}

	@Override
	public boolean intersects(CDateRange other) {
		if (other == null) {
			return false;
		}

		return other.contains(this.getMinValue())
			|| other.contains(this.getMaxValue())
			
			|| this.contains(other.getMinValue())
			|| this.contains(other.getMaxValue());
	}

	public Stream<LocalDate> stream(TemporalUnit unit) {
		return Stream.iterate(getMin(), value -> value.plus(1, unit)).limit(1 + unit.between(getMin(), getMax()));
	}

	@Override
	public String toString() {
		if (isExactly()) {
			return String.format("[%s]", getMin());
		}

		if (isAtLeast()) {
			return String.format("[%s, +\u221E)", getMin());
		}

		if (isAtMost()) {
			return String.format("(-\u221E, %s]", getMax());
		}

		return String.format("[%s, %s]", getMin(), getMax());
	}
}
