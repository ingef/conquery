package com.bakdata.conquery.sql.conversion.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class QueryStepJoiner {

	public static QueryStep joinChildren(
			Iterable<CQElement> children,
			ConversionContext context,
			LogicalOperation logicalOperation,
			DateAggregationAction dateAggregationAction
	) {
		ConversionContext childrenContext = context.createChildContext();

		for (CQElement childNode : children) {
			childrenContext = context.getNodeConversions().convert(childNode, childrenContext);
		}

		List<QueryStep> queriesToJoin = childrenContext.getQuerySteps();
		return joinSteps(queriesToJoin, logicalOperation, dateAggregationAction, context);
	}

	public static QueryStep joinSteps(
			List<QueryStep> queriesToJoin,
			LogicalOperation logicalOperation,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		String joinedCteName = context.getNameGenerator().joinedNodeName(logicalOperation);
		SqlIdColumns ids = coalesceIds(queriesToJoin);
		List<SqlSelect> mergedSelects = mergeSelects(queriesToJoin);
		TableLike<Record> joinedTable = constructJoinedTable(queriesToJoin, logicalOperation, context);

		QueryStep joinedStep;
		QueryStep.QueryStepBuilder joinedStepBuilder = QueryStep.builder()
																.cteName(joinedCteName)
																.fromTable(joinedTable)
																.predecessors(queriesToJoin);

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSteps(queriesToJoin);
		if (dateAggregationAction == DateAggregationAction.BLOCK || dateAggregationDates.dateAggregationImpossible()) {
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.empty(), joinedStepBuilder);
		}
		// if there is only 1 child node containing a validity date, we just keep it as overall validity date for the joined node
		else if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().get(0);
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.of(validityDate), joinedStepBuilder);
		}
		else {
			joinedStep = buildStepAndAggregateDates(ids, mergedSelects, joinedStepBuilder, dateAggregationDates, dateAggregationAction, context);
		}
		return joinedStep;
	}

	public static TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin, LogicalOperation logicalOperation, ConversionContext context) {

		Table<Record> joinedQuery = getIntitialJoinTable(queriesToJoin);

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		JoinType joinType = switch (logicalOperation) {
			case AND -> functionProvider::innerJoin;
			case OR -> functionProvider::fullOuterJoin;
		};

		for (int i = 0; i < queriesToJoin.size() - 1; i++) {

			QueryStep leftPartQS = queriesToJoin.get(i);
			QueryStep rightPartQS = queriesToJoin.get(i + 1);

			SqlIdColumns leftIds = leftPartQS.getQualifiedSelects().getIds();
			SqlIdColumns rightIds = rightPartQS.getQualifiedSelects().getIds();

			List<Condition> joinConditions = SqlIdColumns.join(leftIds, rightIds);

			joinedQuery = joinType.join(joinedQuery, rightPartQS, joinConditions);
		}

		return joinedQuery;
	}

	public static List<SqlSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
						 .flatMap(queryStep -> queryStep.getQualifiedSelects().getSqlSelects().stream())
						 .collect(Collectors.toList());
	}

	public static SqlIdColumns coalesceIds(List<QueryStep> querySteps) {
		List<SqlIdColumns> ids = querySteps.stream().map(QueryStep::getQualifiedSelects).map(Selects::getIds).toList();
		return SqlIdColumns.coalesce(ids);
	}

	private static Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return DSL.table(DSL.name(queriesToJoin.get(0).getCteName()));
	}

	private static QueryStep buildJoinedStep(
			SqlIdColumns ids,
			List<SqlSelect> mergedSelects,
			Optional<ColumnDateRange> validityDate,
			QueryStep.QueryStepBuilder builder
	) {
		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelects(mergedSelects)
								 .validityDate(validityDate)
								 .build();
		return builder.selects(selects).build();
	}

	private static QueryStep buildStepAndAggregateDates(
			SqlIdColumns ids,
			List<SqlSelect> mergedSelects,
			QueryStep.QueryStepBuilder builder,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		List<SqlSelect> withAllValidityDates = new ArrayList<>(mergedSelects);
		withAllValidityDates.addAll(dateAggregationDates.allStartsAndEnds());
		QueryStep joinedStep = buildJoinedStep(ids, withAllValidityDates, Optional.empty(), builder);

		SqlDateAggregator sqlDateAggregator = context.getSqlDialect().getDateAggregator();
		return sqlDateAggregator.apply(
				joinedStep,
				mergedSelects,
				dateAggregationDates,
				dateAggregationAction,
				context
		);
	}

	@FunctionalInterface
	private interface JoinType {
		TableOnConditionStep<Record> join(
				Table<Record> leftPartQueryBase,
				QueryStep rightPartQS,
				List<Condition> joinConditions
		);
	}

}
