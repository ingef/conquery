package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.JoinBranchesCteInput;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;

/**
 * Adapts the legacy connector context to the SQL-ready branch-joining compiler input.
 * <p>
 * Joins the {@link ConceptCteStep#AGGREGATION_SELECT} with the interval packing branch for the aggregated validity date and optional validity date selects
 * {@link IntervalPackingSelectsCte} as well as optional additional predecessors.
 * <p>
 * Joining is optional - if a validity date is not present, the node is excluded from time aggregation or if there is no additional predecessor, no join will
 * take place. See {@link SumSqlAggregator} with distinct-by columns for an example of additional predecessors.
 *
 * <pre>
 *     {@code
 *     "join_branches" as (
 *  	  select
 *  	    coalesce("group_select"."pid", "interval_complete"."pid", "interval_packing_selects"."pid", "row_number_filtered"."pid") as "pid",
 *  	    "interval_complete"."concept_concept-1_validity_date",
 *  	    "interval_packing_selects"."event_duration_sum",
 *  	    "row_number_filtered"."sum_distinct-1"
 *  	  from "group_select"
 *  	    join "interval_complete"
 *  	      on "group_select"."pid" = "interval_complete"."pid"
 *  	    join "interval_packing_selects"
 *    	      on "group_select"."pid" = "interval_packing_selects"."pid"
 *  	    join "row_number_filtered"
 *  	      on "interval_complete"."pid" = "row_number_filtered"."pid"
 *  	)
 *     }
 * </pre>
 */
class JoinBranchesCte extends ConnectorCte {

	@Override
	protected ConceptCteStep cteStep() {
		return ConceptCteStep.JOIN_BRANCHES;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		ConnectorSqlTables tables = tableContext.getConnectorTables();
		List<SqlSelect> eventDateSelects = tableContext.getSqlSelects().stream()
				.flatMap(selects -> selects.getEventDateSelects().stream())
				.toList();
		List<QueryStep> additionalPredecessors = tableContext.allSqlSelects().stream()
				.flatMap(selects -> selects.getAdditionalPredecessor().stream())
				.toList();
		return ConnectorCteCompiler.compileJoinBranches(new JoinBranchesCteInput(
				tableContext.getPrevious(),
				tableContext.getValidityDate(),
				tables,
				tables.isWithIntervalPacking(),
				tables.isExcludedFromTimeAggregation(),
				eventDateSelects,
				additionalPredecessors
		));
	}

}
