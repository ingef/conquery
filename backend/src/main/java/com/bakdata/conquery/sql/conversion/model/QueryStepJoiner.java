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
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class QueryStepJoiner {

	static String PRIMARY_COLUMN_NAME = "primary_column";

	public static ConversionContext joinChildren(
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

		String joinedCteName = constructJoinedQueryStepLabel(queriesToJoin, logicalOperation);
		Field<Object> primaryColumn = coalescePrimaryColumns(queriesToJoin);
		List<SqlSelect> mergedSelects = mergeSelects(queriesToJoin);
		TableLike<Record> joinedTable = constructJoinedTable(queriesToJoin, logicalOperation, context);

		QueryStep joinedStep;
		QueryStep.QueryStepBuilder joinedStepBuilder = QueryStep.builder()
																.cteName(joinedCteName)
																.fromTable(joinedTable)
																.predecessors(queriesToJoin);

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSteps(queriesToJoin);
		if (dateAggregationAction == DateAggregationAction.BLOCK || dateAggregationDates.dateAggregationImpossible()) {
			joinedStep = buildJoinedStep(primaryColumn, mergedSelects, Optional.empty(), joinedStepBuilder);
		}
		// if there is only 1 child node containing a validity date, we just keep it as overall validity date for the joined node
		else if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().get(0);
			joinedStep = buildJoinedStep(primaryColumn, mergedSelects, Optional.of(validityDate), joinedStepBuilder);
		}
		else {
			joinedStep = buildStepAndAggregateDates(primaryColumn, mergedSelects, joinedStepBuilder, dateAggregationDates, dateAggregationAction, context);
		}

		return context.withQueryStep(joinedStep);
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

			Field<Object> leftPartPrimaryColumn = leftPartQS.getQualifiedSelects().getPrimaryColumn();
			Field<Object> rightPartPrimaryColumn = rightPartQS.getQualifiedSelects().getPrimaryColumn();

			joinedQuery = joinType.join(joinedQuery, rightPartQS, leftPartPrimaryColumn, rightPartPrimaryColumn);
		}

		return joinedQuery;
	}

	@FunctionalInterface
	private interface JoinType {
		TableOnConditionStep<Record> join(
				Table<Record> leftPartQueryBase,
				QueryStep rightPartQS,
				Field<Object> leftPartPrimaryColumn,
				Field<Object> rightPartPrimaryColumn
		);
	}

	private static Field<Object> coalescePrimaryColumns(List<QueryStep> querySteps) {
		List<Field<?>> primaryColumns = querySteps.stream()
												  .map(queryStep -> queryStep.getQualifiedSelects().getPrimaryColumn())
												  .collect(Collectors.toList());
		return DSL.coalesce(primaryColumns.get(0), primaryColumns.subList(1, primaryColumns.size()).toArray())
				  .as(PRIMARY_COLUMN_NAME);
	}

	private static List<SqlSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
						 .flatMap(queryStep -> queryStep.getQualifiedSelects().getSqlSelects().stream())
						 .collect(Collectors.toList());
	}

	private static String constructJoinedQueryStepLabel(List<QueryStep> queriesToJoin, LogicalOperation logicalOperation) {

		String labelConnector = switch (logicalOperation) {
			case AND -> "AND";
			case OR -> "OR";
		};

		String concatenatedCteNames = queriesToJoin.stream()
												   .map(QueryStep::getCteName)
												   .collect(Collectors.joining(""));

		return "%s_%8H".formatted(labelConnector, concatenatedCteNames.hashCode());
	}

	private static Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return DSL.table(DSL.name(queriesToJoin.get(0).getCteName()));
	}

	private static QueryStep buildJoinedStep(
			Field<Object> primaryColumn,
			List<SqlSelect> mergedSelects,
			Optional<ColumnDateRange> validityDate,
			QueryStep.QueryStepBuilder builder
	) {
		Selects selects = Selects.builder()
								 .primaryColumn(primaryColumn)
								 .sqlSelects(mergedSelects)
								 .validityDate(validityDate)
								 .build();
		return builder.selects(selects).build();
	}

	private static QueryStep buildStepAndAggregateDates(
			Field<Object> primaryColumn,
			List<SqlSelect> mergedSelects,
			QueryStep.QueryStepBuilder builder,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		List<SqlSelect> withAllValidityDates = new ArrayList<>(mergedSelects);
		withAllValidityDates.addAll(dateAggregationDates.allStartsAndEnds());
		QueryStep joinedStep = buildJoinedStep(primaryColumn, withAllValidityDates, Optional.empty(), builder);

		SqlDateAggregator sqlDateAggregator = context.getSqlDialect().getDateAggregator();
		return sqlDateAggregator.apply(
				joinedStep,
				mergedSelects,
				dateAggregationDates,
				dateAggregationAction,
				context.getNameGenerator()
		);
	}

}
