package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.Iterator;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.google.common.collect.Iterators;

public enum TemporalSelector {
	ANY {
		@Override
		public CDateRange[] sample(CDateSet result) {
			return result.asRanges().toArray(CDateRange[]::new);
		}

		@Override
		public boolean satisfies(boolean[] results) {
			for (boolean value : results) {
				if (value) {
					return true;
				}
			}
			return false;
		}
	},
	ALL {
		@Override
		public CDateRange[] sample(CDateSet result) {
			return result.asRanges().toArray(CDateRange[]::new);
		}

		@Override
		public boolean satisfies(boolean[] results) {
			for (boolean value : results) {
				if (!value) {
					return false;
				}
			}
			return true;
		}
	},
	EARLIEST {
		@Override
		public CDateRange[] sample(CDateSet result) {
			return new CDateRange[]{result.asRanges().iterator().next()};
		}

		@Override
		public boolean satisfies(boolean[] results) {
			return results[0];
		}
	},
	LATEST {
		@Override
		public CDateRange[] sample(CDateSet result) {
			if (result.isEmpty()) {
				return new CDateRange[0];
			}


			final Iterator<CDateRange> iterator = result.asRanges().iterator();

			return new CDateRange[]{Iterators.getLast(iterator)};
		}

		@Override
		public boolean satisfies(boolean[] results) {
			return results[0];
		}
	};

	public abstract CDateRange[] sample(CDateSet result);

	public abstract boolean satisfies(boolean[] results);
}
