package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class StepJoiner {

	static String PRIMARY_COLUMN_NAME = "primary_column";

	public static ConversionContext joinChildren(Iterable<CQElement> children, ConversionContext context, LogicalOperation logicalOperation) {

		ConversionContext childrenContext = context;
		for (CQElement childNode : children) {
			childrenContext = context.getNodeConversions().convert(childNode, childrenContext);
		}

		List<QueryStep> queriesToJoin = childrenContext.getQuerySteps();
		Selects joinedSelects = new Selects(
				coalescePrimaryColumns(queriesToJoin),
				extractValidityDates(queriesToJoin),
				mergeSelects(queriesToJoin)
		);

		QueryStep andQueryStep = QueryStep.builder()
										  .cteName(constructJoinedQueryStepLabel(queriesToJoin, logicalOperation))
										  .selects(joinedSelects)
										  .fromTable(constructJoinedTable(queriesToJoin, logicalOperation, context))
										  .predecessors(queriesToJoin)
										  .build();

		return context.withQuerySteps(List.of(andQueryStep));
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

	private static Optional<ColumnDateRange> extractValidityDates(List<QueryStep> querySteps) {
		// TODO: date aggregation...
		return querySteps.stream()
						 .filter(queryStep -> queryStep.getQualifiedSelects().getValidityDate().isPresent())
						 .map(queryStep -> queryStep.getQualifiedSelects().getValidityDate().get())
						 .findFirst();
	}

	private static List<SqlSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
						 .flatMap(queryStep -> queryStep.getQualifiedSelects().getSqlSelects().stream())
						 .map(FieldWrapper::unique)
						 .collect(Collectors.toList());
	}

	private static String constructJoinedQueryStepLabel(List<QueryStep> queriesToJoin, LogicalOperation logicalOperation) {

		String labelConnector = switch (logicalOperation) {
			case AND -> "_AND_";
			case OR -> "_OR_";
		};

		return queriesToJoin.stream()
							.map(QueryStep::getCteName)
							.collect(Collectors.joining(labelConnector));
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
