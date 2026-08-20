package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.OptionalInt;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Defines constraints on the relation between index and compare
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
sealed public interface TemporalRelationMode permits TemporalRelationMode.After, TemporalRelationMode.Before, TemporalRelationMode.While {

	/**
	 * Take result-dates and convert it to date-restrictions to assert the temporal relation.
	 * @param in the result date
	 * @param selector TemporalSelector helps us optimize selection criteria.
	 */
	CDateRange[] convert(CDateRange[] in, TemporalSelector selector);

	/**
	 * Constraints compare to be after index-period.
	 * <br />
	 * days defines the span of days in which compare may happen:
	 * - days.min is the minimum days, compare needs to be after index
	 * - days.max is the maximum days, compare may be after index
	 * <br />
	 * e.g.
	 * - if index is 2010-01-01, days is {5/10}, then compare must be within {2010-01-06/2010-01-11}
	 * - if index is 2010-01-01, days is {5/+inf}, then compare must be within {2010-01-06/+inf}
	 * - if index is 2010-01-01, days is {+inf/10}, then compare must be within {2010-01-01/2010-01-11}
	 */
	@CPSType(id = "AFTER", base = TemporalRelationMode.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	final class After implements TemporalRelationMode {

		@NotNull
		private final Range.IntegerRange days;


		public CDateRange[] convert(CDateRange[] parts, TemporalSelector selector) {

			if (parts.length == 0) {
				return new CDateRange[0];
			}

			if (selector == TemporalSelector.EARLIEST) {
				CDateRange period = parts[0];

				if (!period.hasLowerBound()) {
					return new CDateRange[0];
				}

				return new CDateRange[]{daysAfter(period.getMinValue(), days)};
			}

			if (selector == TemporalSelector.LATEST) {
				CDateRange period = parts[parts.length - 1];

				if (!period.hasUpperBound()) {
					return new CDateRange[0];
				}

				return new CDateRange[]{daysAfter(period.getMaxValue(), days)};
			}

			CDateRange[] converted = new CDateRange[parts.length];

			for (int index = 0; index < parts.length; index++) {

				final OptionalInt maybeIndexDay = selectIndexDay(parts[index]);

				if (maybeIndexDay.isEmpty()) {
					continue;
				}

				converted[index] = daysAfter(maybeIndexDay.getAsInt(), days);
			}

			return converted;
		}

		private CDateRange daysAfter(int indexDay, Range.IntegerRange days) {
			if (days == null || days.isAll()) {
				return CDateRange.atLeast(indexDay);
			}

			if (!days.isOpen()) {
				return CDateRange.of(indexDay + days.getMin(), indexDay + days.getMax());
			}

			if (days.isAtLeast()) {
				return CDateRange.atLeast(indexDay + days.getMin());
			}

			if (days.isAtMost()) {
				return CDateRange.of(indexDay, indexDay + days.getMax());
			}

			throw new IllegalStateException("Unreachable");
		}

		private OptionalInt selectIndexDay(CDateRange period) {
			if (!period.hasUpperBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(period.getMaxValue());
		}

	}

	/**
	 * Special case of AFTER, with compare and index flipped.
	 * @see Before
	 */
	@CPSType(id = "BEFORE", base = TemporalRelationMode.class)
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	final class Before implements TemporalRelationMode {

		@NotNull
		private final Range.IntegerRange days;

		public CDateRange[] convert(CDateRange[] parts, TemporalSelector selector) {

			if (parts.length == 0) {
				return new CDateRange[0];
			}

			if (selector == TemporalSelector.EARLIEST) {
				CDateRange period = parts[0];

				if (!period.hasLowerBound()) {
					return new CDateRange[0];
				}

				return new CDateRange[]{daysBefore(period.getMinValue(), days)};
			}

			if (selector == TemporalSelector.LATEST) {
				CDateRange period = parts[parts.length - 1];

				if (!period.hasUpperBound()) {
					return new CDateRange[0];
				}

				return new CDateRange[]{daysBefore(period.getMaxValue(), days)};
			}

			CDateRange[] converted = new CDateRange[parts.length];

			for (int index = 0; index < parts.length; index++) {

				final OptionalInt maybeIndexDay = selectIndexDay(parts[index]);

				if (maybeIndexDay.isEmpty()) {
					continue;
				}

				converted[index] = daysBefore(maybeIndexDay.getAsInt(), days);
			}

			return converted;
		}

		private CDateRange daysBefore(int indexDay, Range.IntegerRange days) {
			if (days == null || days.isAll()) {
				return CDateRange.atMost(indexDay);
			}

			if (!days.isOpen()) {
				return CDateRange.of(indexDay - days.getMax(), indexDay - days.getMin());
			}

			if (days.hasLowerBound()) {
				return CDateRange.atMost(indexDay - days.getMin());
			}
			if (days.hasUpperBound()) {
				return CDateRange.of(indexDay - days.getMax(), indexDay);
			}

			throw new IllegalStateException("Unreachable");
		}

		private OptionalInt selectIndexDay(CDateRange period) {
			if (!period.hasLowerBound()) {
				return OptionalInt.empty();
			}

			return OptionalInt.of(period.getMinValue());
		}
	}


	/**
	 * Compare must happen within WHILE, this means, they must at least intersect.
	 */
	@CPSType(id = "WHILE", base = TemporalRelationMode.class)
	@Data
	final class While implements TemporalRelationMode {

		public CDateRange[] convert(CDateRange[] in, TemporalSelector selector) {
			if (in.length == 0) {
				return in;
			}
			return switch (selector) {
				case ANY, ALL -> in;
				case EARLIEST -> new CDateRange[]{in[0]};
				case LATEST -> new CDateRange[]{in[in.length - 1]};
			};
		}
	}

}
