package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

import java.util.*;

public class SubQueryNode extends QPNode {

    private final ConceptQueryPlan plan;
    private final ConceptQueryPlan aggregatorPlan;

    public SubQueryNode(ConceptQueryPlan subplan, ModificationShieldedWorkerStorage storage) {
        this.plan =subplan;
        this.aggregatorPlan = new CloneContext(storage).clone(plan);
    }

    private SubQueryNode(ConceptQueryPlan subplan, ConceptQueryPlan aggragatorPlan) {
        this.plan =subplan;
        this.aggregatorPlan = aggragatorPlan;
    }

    @Override
    public Optional<Boolean> eventFiltersApply(Bucket bucket, int event) {
        return Optional.empty();
    }

    @Override
    public boolean isOfInterest(Entity entity) {
        return plan.isContained();
    }

    @Override
    public void init(Entity entity, QueryExecutionContext context) {
        aggregatorPlan.init(entity, context);
    }

    @Override
    public void nextTable(QueryExecutionContext ctx, Table currentTable) {
        aggregatorPlan.nextTable(ctx, currentTable);
    }

    @Override
    public void nextBlock(Bucket bucket) {
        aggregatorPlan.nextBlock(bucket);
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        aggregatorPlan.getChild().acceptEvent(bucket, event);
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
        return new SubQueryNode(ctx.clone(plan), ctx.clone(aggregatorPlan));
    }

    public List<Aggregator<?>> getSubAggregators() {
        return aggregatorPlan.getAggregators();
    }

    @Override
    public void collectRequiredTables(Set<Table> requiredTables) {
        requiredTables.addAll(aggregatorPlan.collectRequiredTables());
    }
}
