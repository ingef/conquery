package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

/**
 * This aggregator builds a tree of other DateAggregator which is partly parallel to the actual query plan.
 * The aggregator at the top level replaces the previous implementation of the special date union and adds more
 * flexibility through different {@link DateAggregationAction}s.
 */
@RequiredArgsConstructor
public class DateAggregator implements Aggregator<CDateSet> {

    private final DateAggregationAction action;

    private Set<Aggregator<CDateSet>> siblings = new HashSet<>();

    /**
     * Register {@Aggregator<CDateSet>}s from lower levels for the final result generation.
     */
    public void register(Aggregator<CDateSet> sibling) {
        this.siblings.add(sibling);
    }


    /**
     * Register {@Aggregator<CDateSet>}s from lower levels for the final result generation.
     */
    public void registerAll(Collection<Aggregator<CDateSet>> siblings) {
        this.siblings.addAll(siblings);
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        throw new UnsupportedOperationException("This Aggregator uses the result of its siblings and does not accept events");
    }

    @Override
    public CDateSet getAggregationResult() {
        final Set<CDateSet> all = new HashSet<>();
        siblings.forEach(s -> {
            CDateSet result = s.getAggregationResult();
            if(result != null) {
                all.add(result);
            }
        });

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
