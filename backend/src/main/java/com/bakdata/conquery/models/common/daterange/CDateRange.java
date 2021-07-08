package com.bakdata.conquery.models.common.daterange;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class CDateRange implements IRange<LocalDate, CDateRange> {

	/**
	 * Create a Range containing only the supplied date. The value needs to be a valid CDate.
	 * @param value The value this range contains, as {@link CDate}.
	 * @return
	 */
	public static CDateRange exactly(int value) {
		return new CDateRangeExactly(value);
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
		return of(
			CDate.ofLocalDate(value.getMin(), Integer.MIN_VALUE),
			CDate.ofLocalDate(value.getMax(), Integer.MAX_VALUE)
		);
	}
	
	public static CDateRange of(int min, int max) {
		if(min == Integer.MIN_VALUE && max == Integer.MAX_VALUE){
			return CDateRange.all();
		}

		if(max == Integer.MAX_VALUE){
			return atLeast(min);
		}

		if(min == Integer.MIN_VALUE){
			return atMost(max);
		}

		if(min == max){
			return exactly(min);
		}

		return new CDateRangeClosed(min, max);
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
		return new CDateRangeStarting(value);
	}

	/**
	 * Creates a new Range containing all dates before {@code value}, and {@code value}.
	 * @param value the max value of the range, in {@link CDate} format
	 * @return
	 */
	public static CDateRange atMost(int value) {
		return new CDateRangeEnding(value);
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
		return of(
			CDate.ofLocalDate(min, Integer.MIN_VALUE),
			CDate.ofLocalDate(max, Integer.MAX_VALUE)
		);
	}

	/**
	 * Creates a new range containing all valid {@link CDate} values.
	 * @return
	 */
	public static CDateRange all() {
		return CDateRangeOpen.INSTANCE;
	}

	@Override
	public LocalDate getMax() {
		return getMaxValue() == Integer.MAX_VALUE ? null : CDate.toLocalDate(getMaxValue());
	}

	@EqualsAndHashCode.Include
	public abstract int getMaxValue();

	@EqualsAndHashCode.Include
	public abstract int getMinValue();

	@Override
	public LocalDate getMin() {
		return getMinValue() == Integer.MIN_VALUE ? null : CDate.toLocalDate(getMinValue());
	}

	@JsonCreator
	public static CDateRange fromArray(int[] values) {
		if (values.length != 2) {
			throw new IllegalArgumentException("Array must be exactly of size 2");
		}

		return of(values[0], values[1]);
	}

	public CDateRange intersection(CDateRange other) {
		if (!intersects(other)) {
			throw new IllegalArgumentException("Ranges do not intersect.");
		}

		return of(Math.max(getMinValue(), other.getMinValue()), Math.min(getMaxValue(), other.getMaxValue()));
	}

	@JsonValue
	public int[] asArray() {
		return new int[] { getMinValue(), getMaxValue() };
	}


	@Override
	public boolean contains(LocalDate value) {
		return value != null && contains(CDate.ofLocalDate(value));
	}

	@Override
	public boolean contains(CDateRange other) {
		return other != null && contains(other.getMinValue()) && contains(other.getMaxValue());
	}

	public abstract boolean contains(int rep);

	@Override
	public CDateRange span(CDateRange other) {
		return of(Math.min(getMinValue(), other.getMinValue()), Math.max(getMaxValue(), other.getMaxValue()));
	}

	/**
	 * Create a span over ranges ignoring incoming open values, and favoring closed values.
	 *
	 * @param other Date range to span over, may be open.
	 * @return A new closed span.
	 */
	public CDateRange spanClosed(CDateRange other) {
		if(other == null){
			return this;
		}

		int min;
		{
			// Initialize with the lowest know min from this
			if (hasLowerBound()) {
				min = getMinValue();
			}
			else if(hasUpperBound()){
				min = getMaxValue();
			}
			else {
				min = Integer.MAX_VALUE;
			}

			// then compare with others known min - if all fails, set it to Integer.MIN_VALUE
			if (other.hasLowerBound()) {
				min = Math.min(min, other.getMinValue());
			}
			else if (other.hasUpperBound()){
				min = Math.min(min, other.getMaxValue());
			}

			if (min == Integer.MAX_VALUE) {
				min = Integer.MIN_VALUE;
			}
		}

		int max;
		{
			if (hasUpperBound()) {
				max =  getMaxValue();
			}
			else if (hasLowerBound()) {
				max = getMinValue();
			}
			else {
				max = Integer.MIN_VALUE;
			}

			if(other.hasUpperBound()){
				max = Math.max(max,other.getMaxValue());
			}
			else if(other.hasLowerBound()){
				max = Math.max(max, other.getMinValue());
			}

			if (max == Integer.MIN_VALUE) {
				max = Integer.MAX_VALUE;
			}
		}


		if(min == Integer.MIN_VALUE && max != Integer.MAX_VALUE){
			min = max;
		}

		if(max == Integer.MAX_VALUE && min != Integer.MIN_VALUE){
			max = min;
		}

		return of(min, max);
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

		return !(
			this.getMinValue() > other.getMaxValue()
			||
			this.getMaxValue() < other.getMinValue()
		);
	}


	public boolean encloses(CDateRange other) {
		if (other == null) {
			return false;
		}

		return
				getMaxValue() >= other.getMaxValue()
				&& getMinValue() <= other.getMinValue();
	}

	/**
	 * Tests if the Range has an upper bound.
	 * @return {@code true} if the Range has an upper bound
	 */
	public boolean hasUpperBound() {
		return getMaxValue() != Integer.MAX_VALUE;
	}

	/**
	 * Tests if the Range has a lower bound.
	 * @return {@code true} if the Range has a lower bound
	 */
	public boolean hasLowerBound() {
		return getMinValue() != Integer.MIN_VALUE;
	}

	/**
	 * Creates a new {@link Range} that is an equivalent representation of {@code this}.
	 * @return
	 */
	public Range<LocalDate> toSimpleRange() {
		return new Range<>(getMin(), getMax());
	}
	
	/**
	 * The String representation of a DateRange follows the ISO definition.
	 * For open ended ranges a positive or negative ∞ is used. 
	 */
	@Override
	public abstract String toString();

	/**
	 * Returns the years that are part of this date range.
	 *
	 * @return The years as date ranges, from the first date in range to the last in ascending order.
	 */
	public List<CDateRange> getCoveredYears() {
		if(isOpen()){
			// TODO: 22.04.2020 throw exceptiopn?
			return Collections.emptyList();
		}

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
		if(isOpen()){
			// TODO: 22.04.2020 throw exceptiopn?
			return Collections.emptyList();
		}

		// If dateRange is shorter than a quarter, only add that first quarter.
		if(QuarterUtils.getFirstDayOfQuarter(getMin()).isEqual( QuarterUtils.getFirstDayOfQuarter(getMax()))){
			return List.of(this);
		}
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
		if(isOpen()){
			// TODO: 22.04.2020 throw exception?
			return Collections.emptyList();
		}

		List<CDateRange> ranges = new ArrayList<>();
		for(int i = this.getMinValue(); i <= this.getMaxValue(); i++) {
			ranges.add(CDateRange.exactly(i));
		}
		return ranges;
	}

	public boolean isSingleQuarter() {
		if(isOpen()){
			return false;
		}


		int quarterStart = CDate.ofLocalDate(QuarterUtils.getFirstDayOfQuarter(getMinValue()));
		return getMinValue() == quarterStart && getMaxValue() == CQuarter.getLastDay(quarterStart);
	}
}
