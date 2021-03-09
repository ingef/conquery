package com.bakdata.conquery.models.forms.managed;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of the QueryPlan for an {@link EntityDateQuery}.
 */
@RequiredArgsConstructor
public class EntityDateQueryPlan implements QueryPlan {


    private final QueryPlan query;
    private final ArrayConceptQueryPlan features;
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;
    private final CDateRange dateRange;

    @Override
    public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
        // Execute the prerequisite query
        EntityResult preResult = query.execute(ctx, entity);
        if (preResult.isFailed() || !preResult.isContained()) {
            return preResult;
        }
        final List<Object[]> resultLines = new ArrayList<>();
        // It might be a multi line result, so iterate over every result
        for (Object[] line : preResult.asContained().listResultLines()) {

            // Transform the date set of a result line back to the actual result line
            CDateSet entityDate = CDateSet.create();
            entityDate.addAll((CDateSet) line[0]);
            entityDate.retainAll(dateRange);

            // Generate DateContexts in the provided resolutions
            List<DateContext> contexts = new ArrayList<>();
            for (CDateRange range : entityDate.asRanges()) {
                contexts.addAll(DateContext.generateAbsoluteContexts(range, resolutionsAndAlignments));
            }

            FormQueryPlan resolutionQuery = new FormQueryPlan(contexts, features);
            // We assume the date set to be in the first column, this might be wrong
            EntityResult result = resolutionQuery.execute(ctx, entity);

            if (result.isFailed() || !result.isContained()) {
                continue;
            }

            ContainedEntityResult contained = result.asContained();

            resultLines.addAll(contained.listResultLines());

        }
        return new MultilineContainedEntityResult(entity.getId(), resultLines);
    }

    @Override
    public EntityDateQueryPlan clone(CloneContext ctx) {
        return new EntityDateQueryPlan(
                query.clone(ctx),
                features.clone(ctx),
                resolutionsAndAlignments,
                dateRange
        );
    }

    @Override
    public boolean isOfInterest(Entity entity) {
        return query.isOfInterest(entity);
    }
}
