package com.bakdata.conquery.models.forms.managed;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of the QueryPlan for an {@link EntityDateQuery}.
 */
@RequiredArgsConstructor
public class EntityDateQueryPlan implements QueryPlan<MultilineEntityResult> {


    private final QueryPlan query;
    private final ArrayConceptQueryPlan features;
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;
    private final CDateRange dateRestriction;

    private Function<MultilineEntityResult, CDateSet> validityDateCollector;

    @Override
    public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {
        // Execute the prerequisite query
        Optional<EntityResult> preResult = query.execute(ctx, entity);
        if (preResult.isEmpty()) {
            return Optional.empty();
        }
        final List<Object[]> resultLines = new ArrayList<>();

        CDateSet entityDate = query.getValidityDates(preResult.get());
        entityDate.retainAll(dateRestriction);

        // Generate DateContexts in the provided resolutions
        List<DateContext> contexts = new ArrayList<>();
        for (CDateRange range : entityDate.asRanges()) {
            contexts.addAll(DateContext.generateAbsoluteContexts(range, resolutionsAndAlignments));
        }

        FormQueryPlan resolutionQuery = new FormQueryPlan(contexts, features);
        validityDateCollector = resolutionQuery::getValidityDates;

        Optional<MultilineEntityResult> result = resolutionQuery.execute(ctx, entity);

        if (result.isEmpty()) {
            return Optional.empty();
        }

        EntityResult contained = result.get();

        resultLines.addAll(contained.listResultLines());

        return Optional.of(new MultilineEntityResult(entity.getId(), resultLines));
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
    public CDateSet getValidityDates(MultilineEntityResult result) {
        Preconditions.checkNotNull(validityDateCollector, "The query was not executed and no validity date collector set");
        return validityDateCollector.apply(result);
    }
}
