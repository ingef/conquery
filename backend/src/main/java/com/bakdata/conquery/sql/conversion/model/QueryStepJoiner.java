package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.DateAggregationDates;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
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

		ConversionContext childrenContext = context;
		for (CQElement childNode : children) {
			childrenContext = context.getNodeConversions().convert(childNode, childrenContext);
		}

		List<QueryStep> queriesToJoin = childrenContext.getQuerySteps();
		Field<Object> primaryColumn = coalescePrimaryColumns(queriesToJoin);
		List<ExplicitSelect> mergedSelects = mergeSelects(queriesToJoin);

		QueryStep.QueryStepBuilder andQueryStep = QueryStep.builder()
														   .cteName(context.getNameGenerator().joinedNodeName(logicalOperation))
														   .fromTable(constructJoinedTable(queriesToJoin, logicalOperation, context))
														   .conditions(Collections.emptyList())
														   .predecessors(queriesToJoin);

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSteps(queriesToJoin);
		if (dateAggregationAction == DateAggregationAction.BLOCK || dateAggregationDates.dateAggregationImpossible()) {
			Selects selects = Selects.builder()
									 .primaryColumn(primaryColumn)
									 .explicitSelects(mergedSelects)
									 .build();
			andQueryStep = andQueryStep.selects(selects);
			return context.withQuerySteps(List.of(andQueryStep.build()));
		}
		// if there is only 1 child node containing a validity date, we just keep it as overall validity date for the joined node
		else if (dateAggregationDates.getValidityDates().size() == 1) {
			ColumnDateRange validityDate = dateAggregationDates.getValidityDates().get(0);
			Selects selects = Selects.builder()
									 .primaryColumn(primaryColumn)
									 .validityDate(Optional.ofNullable(validityDate))
									 .explicitSelects(mergedSelects)
									 .build();
			andQueryStep = andQueryStep.selects(selects);
			return context.withQuerySteps(List.of(andQueryStep.build()));
		}

		Selects selects = Selects.builder()
								 .primaryColumn(primaryColumn)
								 .sqlSelects(dateAggregationDates.allStartsAndEnds())
								 .explicitSelects(mergedSelects)
								 .build();
		andQueryStep = andQueryStep.selects(selects);

		SqlDateAggregator sqlDateAggregator = context.getSqlDialect().getDateAggregator();
		QueryStep mergeIntervalsStep = sqlDateAggregator.apply(
				andQueryStep.build(),
				mergedSelects,
				dateAggregationDates,
				dateAggregationAction,
				context.getNameGenerator()
		);

		return context.withQuerySteps(List.of(mergeIntervalsStep));
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

	private static Field<Object> coalescePrimaryColumns(List<QueryStep> querySteps) {
		List<Field<?>> primaryColumns = querySteps.stream()
												  .map(queryStep -> queryStep.getQualifiedSelects().getPrimaryColumn())
												  .collect(Collectors.toList());
		return DSL.coalesce(primaryColumns.get(0), primaryColumns.subList(1, primaryColumns.size()).toArray())
				  .as(PRIMARY_COLUMN_NAME);
	}

	private static List<ExplicitSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
						 .flatMap(queryStep -> queryStep.getQualifiedSelects().getExplicitSelects().stream())
						 .collect(Collectors.toList());
	}

	private static Table<Record> getIntitialJoinTable(List<QueryStep> queriesToJoin) {
		return DSL.table(DSL.name(queriesToJoin.get(0).getCteName()));
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

}
