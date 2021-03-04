package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class DateAggregator<R extends Collection<CDateRange>, A extends Aggregator<R>> implements Aggregator<Collection<CDateRange>> {

    private final ConceptQueryPlan.DateAggregationAction action;

    private Set<A> siblings = new HashSet<>();

    /**
     * Register {@link DateAggregator}s from lower levels for the final result generation.
     */
    public void register(Collection<A> siblings) {
        this.siblings.addAll(siblings);
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        throw new UnsupportedOperationException("This Aggregator uses the result of its siblings and does not accepts events");
    }

    @Override
    public Collection<CDateRange> getAggregationResult() {
        final Set<CDateRange> all = new HashSet<>();
        siblings.forEach(s -> all.addAll(s.getAggregationResult()));
        return action.aggregate(all);
    }

    @Override
    public ResultType getResultType() {
        return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
    }

    @Override
    public Aggregator<Collection<CDateRange>> doClone(CloneContext ctx) {
        DateAggregator<R, A> clone = new DateAggregator<>(action);
        Set<A> clonedSiblings = new HashSet<>();
        for (A sibling : siblings) {
            clonedSiblings.add(ctx.clone(sibling));
        }
        clone.siblings = clonedSiblings;
        return clone;
    }
}
