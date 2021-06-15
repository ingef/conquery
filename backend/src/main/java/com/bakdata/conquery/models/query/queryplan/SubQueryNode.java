package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.*;

public class SubQueryNode extends QPNode {

    private final ConceptQueryPlan plan;
    private final ConceptQueryPlan aggregatorPlan;

    private SubQueryNode(ConceptQueryPlan subplan, ModificationShieldedWorkerStorage storage) {
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
        return plan.isOfInterest(entity);
    }

    @Override
    public void init(Entity entity, QueryExecutionContext context) {
        plan.init(entity, context);
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

    public static SubQueryNode create(CQElement child, QueryPlanContext context, ConceptQueryPlan plan, boolean generateDateAggregator){

        // TODO introduce CQSubQuery
        ConceptQueryPlan qp = new ConceptQueryPlan(false);
        if (generateDateAggregator) {
            qp.setDateAggregator(plan.getDateAggregator());
        }
        qp.setChild(child.createQueryPlan(context, qp));
        qp.getDateAggregator().registerAll(qp.getChild().getDateAggregators());
        plan.addSubquery(qp);
        final SubQueryNode subNode = new SubQueryNode(qp, context.getStorage());
        subNode.getSubAggregators().forEach(plan::registerAggregator);
        return subNode;
    }
}
