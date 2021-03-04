package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.daterange.CDateRangeOpen;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Its a placeholder event date Aggregator for tables that don't have an validity date.
 * The result is an open range intersected by a daterestriction
 */
public class OpenEventDateAggregator extends EventDateUnionAggregator {
    private static final Collection<CDateRange> OPEN_SET = Set.of(CDateRangeOpen.INSTANCE);

    private CDateSet dateRestriction;

    public OpenEventDateAggregator() {
        super(Collections.emptySet());
    }

    @Override
    public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
        dateRestriction = ctx.getDateRestriction();
    }

    @Override
    public Aggregator<Collection<CDateRange>> doClone(CloneContext ctx) {
        return new OpenEventDateAggregator();
    }

    @Override
    public Collection<CDateRange> getAggregationResult() {
        if (dateRestriction != null) {
            return dateRestriction.asRanges();
        }
        return OPEN_SET;
    }

    @Override
    public void acceptEvent(Bucket bucket, int event) {
        // Pass
    }
}
