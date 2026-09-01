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
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Function3;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class QueryStepJoiner {

	private static final String NEGATED_CTE_SUFFIX = "_negated";

	/**
	 * Implements an antijoin against the allIdsTable defined in {@link IdColumnConfig#getTable()}, for negation without siblings.
	 */
	public static QueryStep antiJoinWithAllIdsTable(QueryStep queryStep, ConversionContext context, DateAggregationAction dateAggregationAction) {

		SqlFunctionProvider functionProvider = context.getConversionContext().getCompilerDialect().getFunctionProvider();

		Field<String> queryStepPrimaryColumn = queryStep.getQualifiedSelects().getIds().getPrimaryColumn();
		ColumnConfig idColumnConfig = context.getIdColumns().findPrimaryIdColumn();

		String allIdsTable = context.getIdColumns().getTable();

		Field<String> allIdsPrimaryColumn = field(name(allIdsTable, idColumnConfig.getField()), String.class);

		Table<?> table = table(name(allIdsTable))
				.leftOuterJoin(table(name(queryStep.getCteName())))
				.on(allIdsPrimaryColumn.eq(queryStepPrimaryColumn));

		String cteName = queryStep.getCteName() + NEGATED_CTE_SUFFIX;

		Optional<ColumnDateRange> validityDate = switch (dateAggregationAction) {
			case BLOCK, MERGE, INTERSECT -> Optional.of(functionProvider.emptyColumnDateRange());
			case NEGATE -> Optional.of(functionProvider.allRange());
		};

		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(allIdsPrimaryColumn))
								 .validityDate(validityDate.map(cdr -> cdr.asValidityDateRange(cteName)))
								 .build();

		return QueryStep.builder()
						.cteName(cteName)
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
		//TODO somehow Negation does not show up here.

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

	/**
	 * If queriesToJoin contains any negated queries, they will be used in an antijoin against their siblings.
	 *
	 * That means the negation works by removing the entities in the negated portion from the unnegated ones.
	 */
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
			return antiJoinWithAllIdsTable(negateJoined, context, dateAggregationAction);
		}

		String cteName = context.getNameGenerator().joinedNodeName(logicalOperation) + NEGATED_CTE_SUFFIX;
		QueryStep nonNegateJoined = doJoin(withoutNegation, logicalOperation, dateAggregationAction, context);
		QueryStep negateJoined = doJoin(withNegation, logicalOperation, dateAggregationAction, context);

		Selects.SelectsBuilder selects = nonNegateJoined.getQualifiedSelects()
														.toBuilder()
														.sqlSelects(mergeSelects(List.of(nonNegateJoined, negateJoined)));


		if (logicalOperation == ConqueryJoinType.INNER_JOIN) {
			Table<?> table = table(name(nonNegateJoined.getCteName()))
					.leftOuterJoin(table(name(negateJoined.getCteName())))
					.on(nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn()
									   .eq(negateJoined.getQualifiedSelects().getIds().getPrimaryColumn()));

			return QueryStep.builder()
							.cteName(cteName)
							.selects(selects.build())
							.fromTable(table)
							.conditions(List.of(negateJoined.getQualifiedSelects().getIds().getPrimaryColumn().isNull()))
							.predecessors(List.of(nonNegateJoined, negateJoined))
							.build();
		}

		// first, invert dates of negated step
		SqlDateAggregator dateAggregator = context.getCompilerDialect().getDateAggregator();
		negateJoined = dateAggregator.invertAggregatedIntervals(negateJoined, context);

		// join with all-ids table necessary
		ColumnConfig columnConfig = context.getIdColumns().findPrimaryIdColumn();
		Field<Object> allIdsPrimaryColumn = field(name(columnConfig.getField()));
		Field<String> negatePrimaryColumn = negateJoined.getQualifiedSelects().getIds().getPrimaryColumn();
		Field<String> nonNegatePrimaryColumn = nonNegateJoined.getQualifiedSelects().getIds().getPrimaryColumn();

		// prepare date aggregation
		Condition infinityRangeCondition = negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNull());
		DateAggregationDates aggregationDates = DateAggregationDates.forValidityDates(List.of(
				nonNegateJoined.getQualifiedSelects().getValidityDate(),
				negateJoined.getQualifiedSelects().getValidityDate(),
				Optional.of(context.getCompilerDialect().getFunctionProvider().allRangeIf(infinityRangeCondition))
		));
		ColumnDateRange merged =
				dateAggregator.getAggregatedValidityDate(aggregationDates, DateAggregationAction.MERGE)
							  .as(cteName + SharedAliases.DATES_COLUMN.getAlias());

		Field<String> coalescedId = DSL.coalesce(nonNegatePrimaryColumn, allIdsPrimaryColumn)
									   .as(SharedAliases.PRIMARY_COLUMN.getAlias());

		selects = selects
				.ids(new SqlIdColumns(coalescedId))
				.validityDate(Optional.of(merged));

		Table<?> table = table(name(context.getIdColumns().getTable()))
				.leftOuterJoin(table(name(negateJoined.getCteName())))
				.on(allIdsPrimaryColumn.eq(negatePrimaryColumn))
				.leftOuterJoin(table(name(nonNegateJoined.getCteName())))
				.on(allIdsPrimaryColumn.eq(nonNegatePrimaryColumn));

		return QueryStep.builder()
						.cteName(cteName)
						.selects(selects.build())
						.fromTable(table)
						.conditions(List.of(negatePrimaryColumn.isNull().or(nonNegatePrimaryColumn.isNotNull())))
						.predecessors(List.of(nonNegateJoined, negateJoined))
						.build();

	}

	public static TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin, ConqueryJoinType logicalOperation, ConversionContext context) {

		SqlFunctionProvider functionProvider = context.getFunctionProvider();

		Function3<Table<?>, Table<?>, List<Condition>, TableOnConditionStep<Record>> joinType =
				switch (logicalOperation) {
					case INNER_JOIN -> functionProvider::innerJoin;
					case OUTER_JOIN -> functionProvider::fullOuterJoin;
					case LEFT_JOIN -> functionProvider::leftJoin;
				};

		Table<Record> joinedQuery = getIntitialJoinTable(queriesToJoin);

		for (int i = 0; i < queriesToJoin.size() - 1; i++) {

			QueryStep leftPartQS = queriesToJoin.get(i);
			QueryStep rightPartQS = queriesToJoin.get(i + 1);

			SqlIdColumns leftIds = leftPartQS.getQualifiedSelects().getIds();
			SqlIdColumns rightIds = rightPartQS.getQualifiedSelects().getIds();

			List<Condition> joinIdsCondition = leftIds.join(rightIds);

			Condition joinDateCondition = joinOnStratification(leftPartQS, rightPartQS);

			List<Condition> joinConditions = Stream.concat(joinIdsCondition.stream(), Stream.of(joinDateCondition)).collect(Collectors.toList());

			Table<Record> rightPartTable = table(name(rightPartQS.getCteName()));
			joinedQuery = joinType.apply(joinedQuery, rightPartTable, joinConditions);
		}

		return joinedQuery;
	}

	/**
	 * join on stratification date if present
	 */
	private static Condition joinOnStratification(QueryStep leftPartQS, QueryStep rightPartQS) {
		if (leftPartQS.getSelects().getStratificationDate().isEmpty() || rightPartQS.getSelects().getStratificationDate().isEmpty()) {
			//TODO use unconditionalJoin in the future. Hana does not like noCondition joins
			return DSL.noCondition();
		}

		ColumnDateRange leftStratificationDate = leftPartQS.getQualifiedSelects().getStratificationDate().get();
		ColumnDateRange rightStratificationDate = rightPartQS.getQualifiedSelects().getStratificationDate().get();

		return leftStratificationDate.join(rightStratificationDate);
	}

	public static List<SqlSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
						 .flatMap(queryStep -> queryStep.getQualifiedSelects().getSqlSelects().stream())
						 .collect(Collectors.toList());
	}

	public static SqlIdColumns coalesceIds(List<QueryStep> querySteps) {
		List<SqlIdColumns> ids = querySteps.stream().map(QueryStep::getQualifiedSelects).map(Selects::getIds).toList();
		Preconditions.checkArgument(!ids.isEmpty(), "Need at least 1 query step in the list to coalesce Ids");
		return ids.getFirst().coalesce(ids.subList(1, ids.size()));
	}

	private static Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return table(name(queriesToJoin.getFirst().getCteName()));
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

		SqlDateAggregator sqlDateAggregator = context.getCompilerDialect().getDateAggregator();
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


}
