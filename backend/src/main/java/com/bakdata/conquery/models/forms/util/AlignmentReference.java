package com.bakdata.conquery.models.forms.util;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Specifies whether the sub date ranges (which have the maximum length specified by the {@link Resolution})
 * are aligned with regard to beginning or the end of a date range mask. This affects the generated sub date ranges
 * only if the {@link Alignment} is finer than the {@link Resolution}.
 */
public enum AlignmentReference {
	START() {
		@Override
		public List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions) {
			return alignedSubDivisions;
		}

		@Override
		public int getInterestingBorder(CDateRange daterange) {
			return daterange.getMinValue();
		}

		@Override
		public CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder) {
			return CDateRange.of(prioInteressingBorder, lastDaterange.getMaxValue());
		}
	},
	END() {
		@Override
		public List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions) {
			return Lists.reverse(alignedSubDivisions);
		}

		@Override
		public int getInterestingBorder(CDateRange daterange) {
			return daterange.getMaxValue();
		}

		@Override
		public CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder) {
			return CDateRange.of(lastDaterange.getMinValue(), prioInteressingBorder);
		}
	};

	public abstract List<CDateRange> getAlignedIterationDirection(List<CDateRange> alignedSubDivisions);

	public abstract int getInterestingBorder(CDateRange daterange);

	public abstract CDateRange makeMergedRange(CDateRange lastDaterange, int prioInteressingBorder);
}
