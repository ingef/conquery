package com.bakdata.conquery.models.query.queryplan;

import java.util.Iterator;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;

/**
 * The action that is used in certain nodes of the query plan to combine {@link CDateSet}s collected from lower
 * levels of the query plan.
 */
public enum DateAggregationAction {
	/**
	 * Not propagate dates from lower levels to higher.
	 */
	BLOCK() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            return null;
        }
    },
    /**
     * Merge all collected dates from a lower level into a union.
     */
    MERGE() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
			CDateSet combined = CDateSet.createEmpty();
            all.forEach(combined::addAll);
            return combined;
        }
    },
    /**
     * Intersect all collected date sets from lower level with each other.
     */
    INTERSECT() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            if (all.size() < 1) {
				return CDateSet.createEmpty();
            }

            Iterator<CDateSet> it = all.iterator();
            CDateSet intersection = it.next();

            if (all.size() == 1) {
                return intersection;
            }
            // Use the first range as mask and subtract all other ranges from it


            // Intersect
            while (it.hasNext()) {
                final CDateSet next = it.next();
                intersection.retainAll(next);
            }
            return intersection;
        }
    },
    /**
     * Build the negative of the the union of all collected dates
     */
    NEGATE() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            CDateSet negative = CDateSet.createFull();
            all.forEach(negative::removeAll);
            return negative;
        }
    };

    public abstract CDateSet aggregate(Set<CDateSet> all);
}
