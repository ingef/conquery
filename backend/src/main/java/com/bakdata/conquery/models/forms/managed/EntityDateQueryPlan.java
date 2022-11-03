package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of the QueryPlan for an {@link EntityDateQuery}.
 */
@RequiredArgsConstructor
public class EntityDateQueryPlan implements QueryPlan<MultilineEntityResult> {


    private final QueryPlan query;
    private final ArrayConceptQueryPlan features;
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;
    private final CDateRange dateRestriction;


	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		query.init(ctxt, entity);
		features.init(ctxt, entity);
	}

	@Override
    public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

        // Don't set the query date aggregator here because the subqueries should set their aggregator independently

        // Execute the prerequisite query
        Optional<EntityResult> preResult = query.execute(ctx, entity);
        if (preResult.isEmpty()) {
            return Optional.empty();
        }

        Optional<DateAggregator> validityDateAggregator = query.getValidityDateAggregator();
        if (validityDateAggregator.isEmpty()) {
            return Optional.empty();
        }

        final CDateSet aggregationResult = validityDateAggregator.get().createAggregationResult();
        aggregationResult.retainAll(dateRestriction);

        // Generate DateContexts in the provided resolutions
        List<DateContext> contexts = new ArrayList<>();
        for (CDateRange range : aggregationResult.asRanges()) {
            contexts.addAll(DateContext.generateAbsoluteContexts(range, resolutionsAndAlignments));
        }

        FormQueryPlan resolutionQuery = new FormQueryPlan(contexts, features, false);

        return resolutionQuery.execute(ctx, entity);
    }

	@Override
    public boolean isOfInterest(Entity entity) {
        return query.isOfInterest(entity);
    }

    @Override
    public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
        return query.getValidityDateAggregator();
    }
}
