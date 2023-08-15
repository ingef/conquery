package com.bakdata.conquery.sql.conversion.context.step;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.selects.MergedSelects;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

public class StepJoiner {

	public static ConversionContext joinChildren(Iterable<CQElement> children, ConversionContext context, LogicalOperation logicalOperation) {

		ConversionContext childrenContext = context;
		for (CQElement childNode : children) {
			childrenContext = context.getNodeConverterService().convert(childNode, childrenContext);
		}

		List<QueryStep> queriesToJoin = childrenContext.getQuerySteps();
		QueryStep andQueryStep = QueryStep.builder()
										  .cteName(constructJoinedQueryStepLabel(queriesToJoin, logicalOperation))
										  .selects(new MergedSelects(queriesToJoin))
										  .fromTable(constructJoinedTable(queriesToJoin, logicalOperation, context))
										  .conditions(Collections.emptyList())
										  .predecessors(queriesToJoin)
										  .build();

		return context.withQuerySteps(List.of(andQueryStep));
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

	private static TableLike<Record> constructJoinedTable(List<QueryStep> queriesToJoin, LogicalOperation logicalOperation, ConversionContext context) {

		Table<Record> joinedQuery = getIntitialJoinTable(queriesToJoin);

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunction();
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
