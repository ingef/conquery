package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.specific.DateRangeType;
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

	public CDateRange(int min, int max) {
		this.min = min;
		this.max = max;

		if (min > max) {
			throw new IllegalArgumentException(
				String.format("Min(%s) is not less than max(%s)", CDate.toLocalDate(min), CDate.toLocalDate(max)));
		}
	}

	/**
	 * Create a Range containing only the supplied date. The value needs to be a valid CDate.
	 * @param value The value this range contains, as {@link CDate}.
	 * @return
	 */
	public static CDateRange exactly(int value) {
		return new CDateRange(value, value);
	}

	/**
	 * Creates a new Range containing containing only the supplied date.
	 * @param value the value the resulting range will contain.
	 * @return
	 */
	public static CDateRange exactly(LocalDate value) {
		return exactly(CDate.ofLocalDate(value));
	}

	/**
	 * Copy-constructor from {@link Range}.
	 * @param value the Range to copy from.
	 * @return
	 */
	public static CDateRange of(Range<LocalDate> value) {
		return new CDateRange(
			CDate.ofLocalDate(value.getMin(), Integer.MIN_VALUE),
			CDate.ofLocalDate(value.getMax(), Integer.MAX_VALUE)
		);
	}

	/**
	 * Creates a new Range containing all dates after {@code value}, and {@code value}.
	 * @param value the min value of the range
	 * @return
	 */
	public static CDateRange atLeast(LocalDate value) {
		return atLeast(CDate.ofLocalDate(value));
	}

	/**
	 * Creates a new Range containing all dates after {@code value}, and {@code value}.
	 * @param value the min value of the range, in {@link CDate} format
	 * @return
	 */
	public static CDateRange atLeast(int value) {
		return new CDateRange(value, Integer.MAX_VALUE);
	}

	/**
	 * Creates a new Range containing all dates before {@code value}, and {@code value}.
	 * @param value the max value of the range, in {@link CDate} format
	 * @return
	 */
	public static CDateRange atMost(int value) {
		return new CDateRange(Integer.MIN_VALUE, value);
	}

	/**
	 * Creates a new Range containing all dates before {@code value}, and {@code value}.
	 * @param value the min value of the range
	 * @return
	 */
	public static CDateRange atMost(LocalDate value) {
		return atMost(CDate.ofLocalDate(value));
	}


	/**
	 * Creates a new range containing all values between {@code min} and {@code max}.
	 * @param min lower bound of the range
	 * @param max upper bound of the range
	 * @return
	 */
	@JsonCreator
	public static CDateRange of(LocalDate min, LocalDate max) {
		return new CDateRange(
			CDate.ofLocalDate(min, Integer.MIN_VALUE),
			CDate.ofLocalDate(max, Integer.MAX_VALUE)
		);
	}

	/**
	 * Creates a new range containing all valid {@link CDate} values.
	 * @return
	 */
	public static CDateRange all() {
		return new CDateRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public LocalDate getMax() {
		return max == Integer.MAX_VALUE ? null : CDate.toLocalDate(getMaxValue());
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
		return min == Integer.MIN_VALUE ? null : CDate.toLocalDate(getMinValue());
	}

	@JsonCreator
	public static CDateRange fromArray(int[] values) {
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
		return new int[] { min, max };
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

	public static CDateRange spanOf(CDateRange a, CDateRange b) {
		if (a == null) {
			return b;
		}
		else if (b == null) {
			return a;
		}
		else {
			return a.span(b);
		}
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

		return
				other.contains(this.getMinValue())
				|| other.contains(this.getMaxValue())

				|| this.contains(other.getMinValue())
				|| this.contains(other.getMaxValue());
	}

	public boolean isConnected(CDateRange other) {
		if (other == null) {
			return false;
		}

		//either they intersect or they are right next to each other
		return
				this.intersects(other)
				|| this.getMinValue() - 1 == other.getMaxValue()
				|| this.getMaxValue() == other.getMinValue() - 1;
	}

	public boolean encloses(CDateRange other) {
		if (other == null) {
			return false;
		}

		return
				getMaxValue() >= other.getMaxValue()
				&& getMinValue() <= other.getMinValue();
	}

	public Stream<LocalDate> stream(TemporalUnit unit) {
		return Stream.iterate(getMin(), value -> value.plus(1, unit)).limit(1 + unit.between(getMin(), getMax()));
	}

	@Override
	public String toString() {

		if (isAll()) {
			return "-∞/+∞";
		}

		if (isAtLeast()) {
			return String.format("%s/+∞", getMin());
		}

		if (isAtMost()) {
			return String.format("-∞/%s", getMax());
		}

		return String.format("%s/%s", getMin(), getMax());
	}

	/**
	 * Tests if the Range has an upper bound.
	 * @return {@code true} if the Range has an upper bound
	 */
	public boolean hasUpperBound() {
		return max != Integer.MAX_VALUE;
	}

	/**
	 * Tests if the Range has a lower bound.
	 * @return {@code true} if the Range has a lower bound
	 */
	public boolean hasLowerBound() {
		return min != Integer.MIN_VALUE;
	}

	/**
	 * Creates a new {@link Range} that is an equivalent representation of {@code this}.
	 * @return
	 */
	public Range<LocalDate> toSimpleRange() {
		return new Range<>(getMin(), getMax());
	}

	/**
	 * Returns the years that are part of this date range.
	 *
	 * @return The years as date ranges, from the first date in range to the last in ascending order.
	 */
	public List<CDateRange> getCoveredYears() {
		int startYear = this.getMin().getYear();
		int endYear = this.getMax().getYear();

		if(startYear == endYear) {
			return Arrays.asList(this);
		}
		// Range covers multiple years
		List<CDateRange> ranges = new ArrayList<>();
		
		// First year begins with this range
		ranges.add(CDateRange.of(this.getMin(), LocalDate.of(startYear, 12, 31)));
		
		// Years in between
		if(endYear-startYear > 1) {
			ranges.addAll(IntStream
				.rangeClosed(startYear+1, endYear-1)
				// Create date range with first days of year and the last day 
				.mapToObj(year -> CDateRange.of(LocalDate.ofYearDay(year, 1), LocalDate.of(year, 12, 31)))
				.collect(Collectors.toList()));
		}
		// Last year end with this range
		ranges.add(CDateRange.of(LocalDate.of(endYear, 1, 1), this.getMax()));
		return ranges;
	}

	/**
	 * Returns the quarters that are part of this date range.
	 *
	 * @return The quarters as date ranges, from the first date in range to the
	 *         last in ascending order.
	 */
	public List<CDateRange> getCoveredQuarters() {
		List<CDateRange> ranges = new ArrayList<>();
		
		// First quarter begins with this range
		CDateRange start = CDateRange.of(getMin(), QuarterUtils.getLastDayOfQuarter(getMin()));
		CDateRange end = CDateRange.of(QuarterUtils.getFirstDayOfQuarter(getMax()), getMax());
		ranges.add(start);
		LocalDate nextQuarterDate = this.getMin().plus(1, IsoFields.QUARTER_YEARS);
		while(nextQuarterDate.isBefore(end.getMin())) {
			ranges.add(QuarterUtils.fromDate(nextQuarterDate));
			nextQuarterDate = nextQuarterDate.plus(1, IsoFields.QUARTER_YEARS);
		}
		// Don't add the end if its the same quarter as start
		if(!start.equals(end)) {
			// Last year end with this range
			ranges.add(end);
		}

		return ranges;
	}
	
	/**
	 * Returns the days that are part of this date range as ranges.
	 *
	 * @return The days as date ranges, from the first date in range to the
	 *         last in ascending order.
	 */
	public List<CDateRange> getCoveredDays() {

		List<CDateRange> ranges = new ArrayList<>();
		for(int i = this.min; i <= this.max; i++) {
			ranges.add(CDateRange.exactly(i));
		}
		return ranges;
	}
	
	@JsonCreator
	public static CDateRange parse(String value) throws ParsingException {
		return DateRangeType.parseISORange(value);
	}
}
