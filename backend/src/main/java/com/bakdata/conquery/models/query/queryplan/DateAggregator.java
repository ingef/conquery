package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * This aggregator builds a tree of other DateAggregator which is partly parallel to the actual query plan.
 * The aggregator at the top level replaces the previous implementation of the special date union and adds more
 * flexibility through different {@link DateAggregationAction}s.
 */
@RequiredArgsConstructor
@ToString(of = "action")
public class DateAggregator extends Aggregator<CDateSet> {

    private final DateAggregationAction action;

    private final Set<Aggregator<CDateSet>> children = new HashSet<>();

    /**
     * Register {@Aggregator<CDateSet>}s from lower levels for the final result generation.
     */
    public void register(Aggregator<CDateSet> child) {
		children.add(child);
    }


    /**
     * Register {@Aggregator<CDateSet>}s from lower levels for the final result generation.
     */
    public void registerAll(Collection<Aggregator<CDateSet>> children) {
        this.children.addAll(children);
    }

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
    	//TODO don't think this is needed?
		children.forEach(child -> child.init(entity, context));
	}

	@Override
    public void acceptEvent(Bucket bucket, int event) {
        throw new UnsupportedOperationException("This Aggregator uses the result of its siblings and does not accept events");
    }

    @Override
    public CDateSet createAggregationResult() {
        final Set<CDateSet> all = new HashSet<>();
        children.forEach(s -> {
            CDateSet result = s.createAggregationResult();
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

	public boolean hasChildren() {
        return !children.isEmpty();
    }

}
