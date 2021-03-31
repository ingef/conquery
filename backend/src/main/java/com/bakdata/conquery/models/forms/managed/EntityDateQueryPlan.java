package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of the QueryPlan for an {@link EntityDateQuery}.
 */
@RequiredArgsConstructor
public class EntityDateQueryPlan implements QueryPlan<MultilineContainedEntityResult> {


    private final QueryPlan query;
    private final ArrayConceptQueryPlan features;
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;
    private final CDateRange dateRestriction;


    @Override
    public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
        // Execute the prerequisite query
        EntityResult preResult = query.execute(ctx, entity);
        if (preResult.isFailed() || !preResult.isContained()) {
            return preResult;
        }
        final List<Object[]> resultLines = new ArrayList<>();

        CDateSet entityDate = query.collectValidityDates(preResult.asContained());
        entityDate.retainAll(dateRestriction);

        // Generate DateContexts in the provided resolutions
        List<DateContext> contexts = new ArrayList<>();
        for (CDateRange range : entityDate.asRanges()) {
            contexts.addAll(DateContext.generateAbsoluteContexts(range, resolutionsAndAlignments));
        }

        FormQueryPlan resolutionQuery = new FormQueryPlan(contexts, features);

        EntityResult result = resolutionQuery.execute(ctx, entity);

        if (result.isFailed() || !result.isContained()) {
            return result;
        }

        ContainedEntityResult contained = result.asContained();

        resultLines.addAll(contained.listResultLines());

        return new MultilineContainedEntityResult(entity.getId(), resultLines);
    }

    @Override
    public EntityDateQueryPlan clone(CloneContext ctx) {
        return new EntityDateQueryPlan(
                query.clone(ctx),
                features.clone(ctx),
                resolutionsAndAlignments,
                dateRestriction
        );
    }

    @Override
    public boolean isOfInterest(Entity entity) {
        return query.isOfInterest(entity);
    }

	@Override
	public CDateSet collectValidityDates(MultilineContainedEntityResult result) {
		return null;
	}
}
