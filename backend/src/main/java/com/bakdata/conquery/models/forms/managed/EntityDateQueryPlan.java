package com.bakdata.conquery.models.forms.managed;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class EntityDateQueryPlan implements QueryPlan {


    private final QueryPlan query;
    private final ArrayConceptQueryPlan features;

    @Override
    public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
        EntityResult preResult = query.execute(ctx, entity);
        if (preResult.isFailed() || !preResult.isContained()) {
            return preResult;
        }
        final List<DateContext> dateContexts = new ArrayList<>();
        int rangeIndex = 0;
        for( Object[] line : preResult.asContained().listResultLines()) {
            for(CDateRange range : CDateSet.parse((String)line[0]).asRanges() ) {
                DateContext dateContext = new DateContext(range, FeatureGroup.SINGLE_GROUP, ++rangeIndex, null, DateContext.Resolution.COMPLETE);
                dateContexts.add(dateContext);
            }
        }
        FormQueryPlan subPlan = new FormQueryPlan(dateContexts, features);
        return subPlan.execute(ctx, entity);
    }

    @Override
    public EntityDateQueryPlan clone(CloneContext ctx) {
        return new EntityDateQueryPlan(
                query.clone(ctx),
                features.clone(ctx)
        );
    }

    @Override
    public boolean isOfInterest(Entity entity) {
        return query.isOfInterest(entity);
    }
}
