package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Collections;

/**
 * Its a placeholder event date Aggregator for tables that don't have an validity date.
 * The result is an open range intersected by a daterestriction
 */
public class OpenEventDateAggregator extends EventDateUnionAggregator {

    private CDateSet dateRestriction;

    public OpenEventDateAggregator() {
        super(Collections.emptySet());
    }

    @Override
    public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
        dateRestriction = ctx.getDateRestriction();
    }

    @Override
    public Aggregator<CDateSet> doClone(CloneContext ctx) {
        return new OpenEventDateAggregator();
    }

    @Override
    public CDateSet getAggregationResult() {
        if (dateRestriction != null) {
            return dateRestriction;
        }
        return CDateSet.createFull();
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        // Pass
    }
}
