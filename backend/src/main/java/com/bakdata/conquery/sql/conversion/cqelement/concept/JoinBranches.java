package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumDistinctSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/**
 * Joins the {@link ConnectorCteStep#AGGREGATION_SELECT} with the interval packing branch for the aggregated validity date and optional additional predecessors.
 * <p>
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
class JoinBranches extends ConnectorCte {

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.JOIN_BRANCHES;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(tableContext.getPrevious());

		Optional<ColumnDateRange> validityDate =
				Optional.of(tableContext)
						.filter(context -> context.getValidityDate().isPresent() && !context.isExcludedFromDateAggregation())
						.map(JoinBranches::applyIntervalPacking)
						.map(finalIntervalPackingStep -> {
							queriesToJoin.add(finalIntervalPackingStep);
							return finalIntervalPackingStep;
						})
						.flatMap(finalIntervalPackingStep -> finalIntervalPackingStep.getQualifiedSelects().getValidityDate());

		tableContext.allSqlSelects().stream()
					.flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessor().stream())
					.forEach(queriesToJoin::add);

		Field<Object> primaryColumn = QueryStepJoiner.coalescePrimaryColumns(queriesToJoin);
		List<SqlSelect> mergedSqlSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		Selects selects = Selects.builder()
								 .primaryColumn(primaryColumn)
								 .validityDate(validityDate)
								 .sqlSelects(mergedSqlSelects)
								 .build();

		TableLike<Record> fromTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.AND, tableContext.getConversionContext());

		return QueryStep.builder()
						.selects(selects)
						.fromTable(fromTable)
						.predecessors(queriesToJoin);
	}

	private static QueryStep applyIntervalPacking(CQTableContext tableContext) {

		String conceptLabel = tableContext.getConceptLabel();
		IntervalPackingTables intervalPackingTables =
				IntervalPackingTables.forConcept(conceptLabel, tableContext.getConnectorTables(), tableContext.getNameGenerator());

		IntervalPackingContext intervalPackingContext =
				IntervalPackingContext.builder()
									  .nodeLabel(conceptLabel)
									  .primaryColumn(tableContext.getPrimaryColumn())
									  .validityDate(tableContext.getValidityDate().get())
									  .intervalPackingTables(intervalPackingTables)
									  .build();

		return tableContext.getConversionContext()
						   .getSqlDialect()
						   .getIntervalPacker()
						   .createIntervalPackingSteps(intervalPackingContext);
	}

}
