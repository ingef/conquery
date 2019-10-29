package com.bakdata.conquery.models.common;

import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Wither;

@Wither
@Getter
@EqualsAndHashCode
public class Range<T extends Comparable> implements IRange<T, Range<T>>{

	private final T min;
	private final T max;

	public Range(T min, T max){
		this.min = min;
		this.max = max;

		if(!isOrdered()) {
			throw new IllegalArgumentException(String.format("min '%s' is not less than max '%s'", min, max));
		}
	}

	@Override
	public String toString() {
		if (isExactly()) {
			return String.format("[%s]", getMin());
		}

		if (isAtLeast()) {
			return String.format("[%s, +∞)", getMin());
		}

		if (isAtMost()) {
			return String.format("(-∞, %s]", getMax());
		}

		return String.format("[%s, %s]", getMin(), getMax());
	}

	public static <T extends Comparable<?>> Range<T> exactly(T exactly) {
		return new Range<>(exactly, exactly);
	}

	@JsonCreator
	public static <T extends Comparable<?>> Range<T> of(@JsonProperty("min") T min, @JsonProperty("max") T max) {
		return new Range<>(min, max);
	}

	public static <T extends Comparable<?>> Range<T> atMost(T bound) {
		return new Range<>(null, bound);
	}

	public static <T extends Comparable<?>> Range<T> atLeast(T bound) {
		return new Range<>(bound, null);
	}

	public static <T extends Comparable<?>> Range<T> all() {
		return new Range<>(null, null);
	}

	@Override
	@JsonIgnore
	public boolean isExactly() {
		return min != null && max != null && min.compareTo(max) == 0;
	}

	@Override
	@JsonIgnore
	public boolean isAtLeast() {
		return min != null && max == null;
	}

	@Override
	@JsonIgnore
	public boolean isAtMost() {
		return max != null && min == null;
	}

	@Override
	@JsonIgnore
	public boolean isAll() {
		return max == null && min == null;
	}

	@Override
	@JsonIgnore
	public boolean isOpen() {
		return max == null || min == null;
	}

	@Override
	public boolean contains(Range<T> other) {
		if (other == null) {
			return false;
		}

		if (this.equals(other)) {
			return true;
		}

		if (isAtLeast()) {
			if (other.isAtMost() || other.isAll()) {
				return false;
			}

			return contains(other.getMin());
		}

		if (isAtMost()) {
			if (other.isAtLeast() || other.isAll()) {
				return false;
			}

			return contains(other.getMax());
		}

		return contains(other.getMin()) && contains(other.getMax());
	}

	@ValidationMethod(message = "If a range is not open in one direction, min needs to be less or equal to max")
	@JsonIgnore
	public final boolean isOrdered() {
		return isOpen() || min.compareTo(max) <= 0;
	}


	@Override
	public Range<T> span(@NonNull Range<T> other) {
		Range<T> out = this;

		if (this.getMax() != null && (other.getMax() == null || other.getMax().compareTo(this.getMax()) > 0)) {
			out = withMax(other.getMax());
		}

		if (out.getMin() != null && (other.getMin() == null || other.getMin().compareTo(this.getMin()) < 0)) {
			out = out.withMin(other.getMin());
		}

		return out;
	}

	@Override
	public boolean contains(T value) {
		if(value == null) {
			return false;
		}

		if (getMin() != null && value.compareTo(getMin()) < 0) {
			return false;
		}

		return getMax() == null || value.compareTo(getMax()) <= 0;
	}

	public static class IntegerRange extends Range<Integer> {
		public IntegerRange(Integer min, Integer max) {
			super(min, max);
		}

		public static IntegerRange fromNumberRange(IRange<? extends Number, ?> orig){
			return new Range.IntegerRange(
				Optional.ofNullable(orig.getMin()).map(Number::intValue).orElse(null),
				Optional.ofNullable(orig.getMax()).map(Number::intValue).orElse(null));
		}

		@Override
		public boolean contains(Integer value) {
			return value != null && contains(value.intValue());
		}

		public boolean contains(Number value) {
			return value != null && contains(value.intValue());
		}

		public boolean contains(int value) {
			if(getMin() != null && value < getMin()) {
				return false;
			}
			if(getMax() != null && value > getMax()) {
				return false;
			}
			return true;
		}
	}

	public static class LongRange extends Range<Long> {
		public LongRange (Long min, Long max) {
			super(min, max);
		}

		public static LongRange fromNumberRange(IRange<? extends Number, ?> orig){
			return new Range.LongRange(
				Optional.ofNullable(orig.getMin()).map(Number::longValue).orElse(null),
				Optional.ofNullable(orig.getMax()).map(Number::longValue).orElse(null));
		}

		@Override
		public boolean contains(Long value) {
			return value != null && contains(value.longValue());
		}

		public boolean contains(Number value) {
			return value != null && contains(value.longValue());
		}

		public boolean contains(long value) {
			if(getMin() != null && value < getMin()) {
				return false;
			}
			if(getMax() != null && value > getMax()) {
				return false;
			}
			return true;
		}
	}

	public static class FloatRange extends Range<Float> {
		public FloatRange(Float min, Float max) {
			super(min, max);
		}

		public static FloatRange fromNumberRange(IRange<? extends Number, ?> orig){
			return new Range.FloatRange(
				Optional.ofNullable(orig.getMin()).map(Number::floatValue).orElse(null),
				Optional.ofNullable(orig.getMax()).map(Number::floatValue).orElse(null));
		}

		@Override
		public boolean contains(Float value) {
			return value != null && contains(value.floatValue());
		}

		public boolean contains(Number value) {
			return value != null && contains(value.floatValue());
		}

		public boolean contains(float value) {
			if(getMin() != null && value < getMin()) {
				return false;
			}
			if(getMax() != null && value > getMax()) {
				return false;
			}
			if(Float.isNaN(value)) {
				return false;
			}
			return true;
		}
	}

	public static class DoubleRange extends Range<Double> {
		public DoubleRange(Double min, Double max) {
			super(min, max);
		}

		public static DoubleRange fromNumberRange(IRange<? extends Number, ?> orig){
			return new Range.DoubleRange(
				Optional.ofNullable(orig.getMin()).map(Number::doubleValue).orElse(null),
				Optional.ofNullable(orig.getMax()).map(Number::doubleValue).orElse(null));
		}

		@Override
		public boolean contains(Double value) {
			return value != null && contains(value.doubleValue());
		}

		public boolean contains(Number value) {
			return value != null && contains(value.doubleValue());
		}

		public boolean contains(double value) {
			if(getMin() != null && value < getMin()) {
				return false;
			}
			if(getMax() != null && value > getMax()) {
				return false;
			}
			if(Double.isNaN(value)) {
				return false;
			}
			return true;
		}
	}
}
