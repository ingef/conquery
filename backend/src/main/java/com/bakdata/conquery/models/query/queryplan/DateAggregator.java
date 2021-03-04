package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class DateAggregator implements Aggregator<CDateSet> {

    private final ConceptQueryPlan.DateAggregationAction action;

    private Set<Aggregator<CDateSet>> siblings = new HashSet<>();

    /**
     * Register {@link DateAggregator}s from lower levels for the final result generation.
     */
    public void register(Collection<Aggregator<CDateSet>> siblings) {
        this.siblings.addAll(siblings);
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        throw new UnsupportedOperationException("This Aggregator uses the result of its siblings and does not accepts events");
    }

    @Override
    public CDateSet getAggregationResult() {
        CDateSet ret = CDateSet.create();
        final Set<CDateSet> all = new HashSet<>();
        siblings.forEach(s -> all.add(s.getAggregationResult()));

        // Repackage to get the results sorted. Might need some optimization.
        return action.aggregate(all);
    }

    @Override
    public ResultType getResultType() {
        return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
    }

    @Override
    public Aggregator<CDateSet> doClone(CloneContext ctx) {
        DateAggregator clone = new DateAggregator(action);
        Set<Aggregator<CDateSet>> clonedSiblings = new HashSet<>();
        for (Aggregator<CDateSet> sibling : siblings) {
            clonedSiblings.add(ctx.clone(sibling));
        }
        clone.siblings = clonedSiblings;
        return clone;
    }
}
