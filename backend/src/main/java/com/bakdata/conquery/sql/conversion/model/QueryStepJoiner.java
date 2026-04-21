package com.bakdata.conquery.sql.conversion.model;

import static org.jooq.impl.DSL.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.PostgreSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class QueryStepJoiner {

	private static final String NEGATED_CTE_SUFFIX = "_negated";

	public static QueryStep antiJoinWithAllIdsTable(QueryStep queryStep, ConversionContext context) {

		SqlFunctionProvider functionProvider = context.getConversionContext().getDialectBundle().getFunctionProvider();

		Field<Object> queryStepPrimaryColumn = queryStep.getQualifiedSelects().getIds().getPrimaryColumn();
		ColumnConfig idColumnConfig = context.getIdColumns().findPrimaryIdColumn();

		String allIdsTable = context.getIdColumns().getTable();

		Field<Object> allIdsPrimaryColumn = field(name(allIdsTable, idColumnConfig.getField()));

		Table<?> table = table(name(allIdsTable))
				.leftOuterJoin(table(name(queryStep.getCteName())))
				.on(allIdsPrimaryColumn.eq(queryStepPrimaryColumn));

		// TODO
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(allIdsPrimaryColumn))
								 // negation results in +/-inf date for all entries that match the anti-join condition
								 .validityDate(Optional.of(functionProvider.maxRange()))
								 .build();

		return QueryStep.builder()
						.cteName(queryStep.getCteName() + NEGATED_CTE_SUFFIX)
						.selects(selects)
						.fromTable(table)
						.conditions(List.of(queryStepPrimaryColumn.isNull()))
						.predecessor(queryStep)
						.build();
	}

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
		// keep all entries from group A (non-negate steps), whose entity has no matching entry in group B (negate steps)
		if (queriesToJoin.stream().anyMatch(QueryStep::isNegate)) {
			return joinStepsContainingNegation(queriesToJoin, logicalOperation, dateAggregationAction, context);
		}

		// splitting out the actual join to be able to recur here
		return doJoin(queriesToJoin, logicalOperation, dateAggregationAction, context);
	}

	private static QueryStep doJoin(
			List<QueryStep> queriesToJoin,
			ConqueryJoinType logicalOperation,
			DateAggregationAction dateAggregationAction,
			ConversionContext context) {
		// no join required
		if (queriesToJoin.size() == 1) {
			return queriesToJoin.getFirst();
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
			// for forms, date aggregation is allways blocked, but dates need to be coalesced in case we do a fulll outer join
			Optional<ColumnDateRange> stratificationDate = coalesceStratificationDates(queriesToJoin);
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.empty(), stratificationDate, joinedStepBuilder);
		}
		// if there is only 1 child node containing a validity date, we just keep it as overall validity date for the joined node
		else if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().getFirst();
			joinedStep = buildJoinedStep(ids, mergedSelects, Optional.of(validityDate), Optional.empty(), joinedStepBuilder);
		}
		else {
			joinedStep = buildStepAndAggregateDates(ids, mergedSelects, joinedStepBuilder, dateAggregationDates, dateAggregationAction, context);
		}
		return joinedStep;
	}

	private static QueryStep joinStepsContainingNegation(
			List<QueryStep> queriesToJoin,
			ConqueryJoinType logicalOperation,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		Map<Boolean, List<QueryStep>> byNegation = queriesToJoin.stream().collect(Collectors.groupingBy(QueryStep::isNegate));

		List<QueryStep> withoutNegation = byNegation.get(false);
		List<QueryStep> withNegation = byNegation.get(true);

		if (withoutNegation == null) {
			QueryStep negateJoined = doJoin(withNegation, logicalOperation, dateAggregationAction, context);
			return antiJoinWithAllIdsTable(negateJoined, context);
		}

		String cteName = context.getNameGenerator().joinedNodeName(logicalOperation) + NEGATED_CTE_SUFFIX;
		QueryStep nonNegateJoined = doJoin(withoutNegation, logicalOperation, dateAggregationAction, context);
		QueryStep negateJoined = doJoin(withNegation, logicalOperation, dateAggregationAction, context);

		Selects selects = nonNegateJoined.getQualifiedSelects()
										 .toBuilder()
										 .sqlSelects(mergeSelects(List.of(nonNegateJoined, negateJoined)))
										 .build();

		Table<?> table;
		Condition joinCondition;
		if (logicalOperation == ConqueryJoinType.INNER_JOIN) {
			table = table(name(nonNegateJoined.getCteName()))
					.leftOuterJoin(table(name(negateJoined.getCteName())))
					.on(nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn()
									   .eq(negateJoined.getQualifiedSelects().getIds().getPrimaryColumn()));

			joinCondition = negateJoined.getQualifiedSelects().getIds().getPrimaryColumn().isNull();
		}
		else {
			// first, invert dates of negated step
			negateJoined = context.getDialectBundle().getDateAggregator().invertAggregatedIntervals(negateJoined, context);

			// join with all-ids table necessary
			ColumnConfig columnConfig = context.getIdColumns().findPrimaryIdColumn();
			Field<Object> allIdsPrimaryColumn = field(name(columnConfig.getField()));
			Field<Object> negatePrimaryColumn = negateJoined.getQualifiedSelects().getIds().getPrimaryColumn();
			Field<Object> nonNegatePrimaryColumn = nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn();

			// prepare date aggregation
			Condition infinityRangeCondition = negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNull());
			DateAggregationDates aggregationDates = DateAggregationDates.forValidityDates(List.of(
					nonNegateJoined.getQualifiedSelects().getValidityDate(),
					negateJoined.getQualifiedSelects().getValidityDate(),
					Optional.of(context.getDialectBundle().getFunctionProvider().maxRangeIf(infinityRangeCondition))
			));
			SqlDateAggregator dateAggregator = context.getDialectBundle().getDateAggregator();
			ColumnDateRange merged =
					dateAggregator.getAggregatedValidityDate(aggregationDates, DateAggregationAction.MERGE)
								  .as(cteName + SharedAliases.DATES_COLUMN.getAlias());

			Field<Object> coalescedId = DSL.coalesce(nonNegatePrimaryColumn, allIdsPrimaryColumn)
										   .as(SharedAliases.PRIMARY_COLUMN.getAlias());

			selects = selects.toBuilder()
							 .ids(new SqlIdColumns(coalescedId))
							 .validityDate(Optional.of(merged))
							 .build();

			table = table(name(context.getIdColumns().getTable()))
					.leftOuterJoin(table(name(negateJoined.getCteName())))
					.on(allIdsPrimaryColumn.eq(negatePrimaryColumn))
					.leftOuterJoin(table(name(nonNegateJoined.getCteName())))
					.on(allIdsPrimaryColumn.eq(nonNegatePrimaryColumn));

			joinCondition = negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNotNull());
		}

		return QueryStep.builder()
						.cteName(cteName)
						.selects(selects)
						.fromTable(table)
						.conditions(List.of(joinCondition))
						.predecessors(List.of(nonNegateJoined, negateJoined))
						.build();
	}

	public static TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin, ConqueryJoinType logicalOperation, ConversionContext context) {
		Table<Record> joinedQuery = getIntitialJoinTable(queriesToJoin);

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
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

			Table<Record> rightPartTable = table(name(rightPartQS.getCteName()));
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
		return table(name(queriesToJoin.get(0).getCteName()));
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

		SqlDateAggregator sqlDateAggregator = context.getDialectBundle().getDateAggregator();
		return sqlDateAggregator.apply(
				joinedStep,
				mergedSelects,
				dateAggregationDates,
				dateAggregationAction,
				context
		);
	}

	private static Optional<ColumnDateRange> coalesceStratificationDates(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
							.map(QueryStep::getQualifiedSelects)
							.map(Selects::getStratificationDate)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.reduce(ColumnDateRange::coalesce);
	}

	@FunctionalInterface
	private interface JoinType {
		TableOnConditionStep<Record> join(Table<?> leftPart, Table<?> rightPart, List<Condition> joinConditions);
	}

}
