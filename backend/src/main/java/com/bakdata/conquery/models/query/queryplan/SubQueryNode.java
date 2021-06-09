package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
public class SubQueryNode extends QPNode {

    private final ConceptQueryPlan plan;

    @Override
    public Optional<Boolean> eventFiltersApply(Bucket bucket, int event) {
        return Optional.empty();
    }

    @Override
    public boolean isOfInterest(Entity entity) {
        return plan.isContained();
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        // Do nothing, the sub query is executed already
    }

    @Override
    public Optional<Boolean> aggregationFiltersApply() {
        return Optional.of(plan.isContained());
    }

    @Override
    public Collection<Aggregator<CDateSet>> getDateAggregators() {
        return Collections.emptyList();
    }

    @Override
    public QPNode doClone(CloneContext ctx) {
        return new SubQueryNode(ctx.clone(plan));
    }
}
