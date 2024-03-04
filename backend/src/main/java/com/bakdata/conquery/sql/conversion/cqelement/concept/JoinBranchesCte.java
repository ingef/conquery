package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumDistinctSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Record;
import org.jooq.TableLike;

/**
 * Joins the {@link ConnectorCteStep#AGGREGATION_SELECT} with the interval packing branch for the aggregated validity date and optional additional predecessors.
 * <p>
 * Joining is optional - if a validity date is not present, the node is excluded from time aggregation or if there is no additional predecessor, no join will
 * take place. See {@link SumDistinctSqlAggregator} for an example of additional predecessors.
 *
 * <pre>
 *     {@code
 *     "join_branches" as (
 *  	  select
 *  	    coalesce("group_select"."pid", "interval_complete"."pid", "row_number_filtered"."pid") as "pid",
 *  	    "interval_complete"."concept_concept-1_validity_date",
 *  	    "row_number_filtered"."sum_distinct-1"
 *  	  from "group_select"
 *  	    join "interval_complete"
 *  	      on "group_select"."pid" = "interval_complete"."pid"
 *  	    join "row_number_filtered"
 *  	      on "interval_complete"."pid" = "row_number_filtered"."pid"
 *  	)
 *     }
 * </pre>
 */
class JoinBranchesCte extends ConnectorCte {

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.JOIN_BRANCHES;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(tableContext.getPrevious());

		Optional<ColumnDateRange> validityDate;
		if (tableContext.getIntervalPackingContext().isEmpty()) {
			validityDate = Optional.empty();
		}
		else {
			IntervalPacker intervalPacker = tableContext.getParentContext().getSqlDialect().getIntervalPacker();
			QueryStep lastIntervalPackingStep = intervalPacker.createIntervalPackingSteps(tableContext.getIntervalPackingContext().get());
			queriesToJoin.add(lastIntervalPackingStep);
			validityDate = lastIntervalPackingStep.getQualifiedSelects().getValidityDate();
		}

		tableContext.allSqlSelects().stream()
					.flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessor().stream())
					.forEach(queriesToJoin::add);

		SqlIdColumns ids = QueryStepJoiner.coalesceIds(queriesToJoin);
		List<SqlSelect> mergedSqlSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		Selects selects = Selects.builder()
								 .ids(ids)
								 .validityDate(validityDate)
								 .sqlSelects(mergedSqlSelects)
								 .build();

		TableLike<Record> fromTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.AND, tableContext.getParentContext());

		return QueryStep.builder()
						.selects(selects)
						.fromTable(fromTable)
						.predecessors(queriesToJoin);
	}

}
