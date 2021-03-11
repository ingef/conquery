package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.CDateSet;

import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public enum DateAggregationAction {
    BLOCK() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            return CDateSet.create();
        }
    },
    MERGE() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            CDateSet combined = CDateSet.create();
            all.forEach(combined::addAll);
            return combined;
        }
    },
    INTERSECT() {
        @Override
        public CDateSet aggregate(Set<CDateSet> all) {
            if (all.size() < 1) {
                return CDateSet.create();
            }

            Iterator<CDateSet> it = all.iterator();
            CDateSet intersection = it.next();

            if (all.size() == 1) {
                return intersection;
            }
            // Use the first range as mask and subtract all other ranges from it


            // Intersect
            while (it.hasNext()) {
                intersection.retainAll(it.next());
            }
            return intersection;
        }
    },
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
