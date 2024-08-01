package com.bakdata.conquery.sql.conversion.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
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
			ConqueryJoinType logicalOperation,
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
			ConqueryJoinType logicalOperation,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		// no join required
		if (queriesToJoin.size() == 1) {
			return queriesToJoin.get(0);
		}

		String joinedNodeName = context.getNameGenerator().joinedNodeName(logicalOperation);
		SqlIdColumns ids = coalesceIds(queriesToJoin);
		List<SqlSelect> mergedSelects = mergeSelects(queriesToJoin);
		TableLike<Record> joinedTable = constructJoinedTable(queriesToJoin, logicalOperation, context);

		QueryStep joinedStep;
		QueryStep.QueryStepBuilder joinedStepBuilder = QueryStep.builder()
																.cteName(joinedNodeName)
																.fromTable(joinedTable)
																.predecessors(queriesToJoin);

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSteps(queriesToJoin);
		if (dateAggregationAction == DateAggregationAction.BLOCK || dateAggregationDates.dateAggregationImpossible()) {
			// for forms, date aggregation is allways blocked // TODO check if this is really correct
			Optional<ColumnDateRange> stratificationDate = queriesToJoin.get(0).getQualifiedSelects().getStratificationDate();
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.empty(), stratificationDate, joinedStepBuilder);
		}
		// if there is only 1 child node containing a validity date, we just keep it as overall validity date for the joined node
		else if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().get(0);
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.of(validityDate), Optional.empty(), joinedStepBuilder);
		}
		else {
			joinedStep = buildStepAndAggregateDates(ids, mergedSelects, joinedStepBuilder, dateAggregationDates, dateAggregationAction, context);
		}
		return joinedStep;
	}

	public static TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin, ConqueryJoinType logicalOperation, ConversionContext context) {
		Table<Record> joinedQuery = getIntitialJoinTable(queriesToJoin);

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		JoinType joinType = switch (logicalOperation) {
			case INNER_JOIN -> functionProvider::innerJoin;
			case OUTER_JOIN -> functionProvider::fullOuterJoin;
			case LEFT_JOIN -> functionProvider::leftJoin;
		};

		for (int i = 0; i < queriesToJoin.size() - 1; i++) {

			QueryStep leftPartQS = queriesToJoin.get(i);
			QueryStep rightPartQS = queriesToJoin.get(i + 1);

			SqlIdColumns leftIds = leftPartQS.getQualifiedSelects().getIds();
			SqlIdColumns rightIds = rightPartQS.getQualifiedSelects().getIds();

			List<Condition> joinIdsCondition = leftIds.join(rightIds);

			Condition joinDateCondition = DSL.noCondition();
			// join on stratification date if present
			if (leftPartQS.getSelects().getStratificationDate().isPresent() && rightPartQS.getSelects().getStratificationDate().isPresent()) {
				ColumnDateRange leftStratificationDate = leftPartQS.getQualifiedSelects().getStratificationDate().get();
				ColumnDateRange rightStratificationDate = rightPartQS.getQualifiedSelects().getStratificationDate().get();
				joinDateCondition = leftStratificationDate.join(rightStratificationDate);
			}

			List<Condition> joinConditions = Stream.concat(joinIdsCondition.stream(), Stream.of(joinDateCondition)).collect(Collectors.toList());

			Table<Record> rightPartTable = DSL.table(DSL.name(rightPartQS.getCteName()));
			joinedQuery = joinType.join(joinedQuery, rightPartTable, joinConditions);
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
		Preconditions.checkArgument(!ids.isEmpty(), "Need at least 1 query step in the list to coalesce Ids");
		return ids.get(0).coalesce(ids.subList(1, ids.size()));
	}

	private static Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return DSL.table(DSL.name(queriesToJoin.get(0).getCteName()));
	}

	private static QueryStep buildJoinedStep(
			SqlIdColumns ids,
			List<SqlSelect> mergedSelects,
			Optional<ColumnDateRange> validityDate,
			Optional<ColumnDateRange> stratificationDate,
			QueryStep.QueryStepBuilder builder
	) {
		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(stratificationDate)
								 .validityDate(validityDate)
								 .sqlSelects(mergedSelects)
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
		QueryStep joinedStep = buildJoinedStep(ids, withAllValidityDates, Optional.empty(), Optional.empty(), builder);

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
		TableOnConditionStep<Record> join(Table<?> leftPart, Table<?> rightPart, List<Condition> joinConditions);
	}

}
