package com.bakdata.conquery.sql.conversion.model;

import java.util.List;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.model.schema.EntitySchema;

/**
 * Adapts legacy backend query nodes and ID configuration to connector-owned query-step composition.
 *
 * <p>TODO Remove this adapter once the backend conversion entry point consumes resolved query nodes and an
 * {@link EntitySchema} directly.</p>
 */
public final class QueryStepComposer {

	private QueryStepComposer() {
	}

	public static QueryStep antiJoinWithAllIdsTable(
			QueryStep queryStep,
			ConversionContext context,
			DateAggregationAction dateAggregationAction
	) {
		return com.bakdata.conquery.sql.compiler.ir.QueryStepComposer.antiJoinWithAllEntities(
				queryStep,
				EntitySchemaAdapter.from(context.getIdColumns()),
				dateAggregationAction,
				context.getCompilerDialect()
		);
	}

	public static List<QueryStep> convertChildren(
			Iterable<CQElement> children,
			ConversionContext context
	) {
		ConversionContext childrenContext = context.createChildContext();
		for (CQElement childNode : children) {
			childrenContext = context.getNodeConversions().convert(childNode, childrenContext);
		}
		return childrenContext.getQuerySteps();
	}

	public static QueryStep joinSteps(
			List<QueryStep> queriesToJoin,
			JoinMode logicalOperation,
			DateAggregationAction dateAggregationAction,
			ConversionContext context
	) {
		return com.bakdata.conquery.sql.compiler.ir.QueryStepComposer.joinSteps(
				queriesToJoin,
				logicalOperation,
				dateAggregationAction,
				EntitySchemaAdapter.from(context.getIdColumns()),
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
	}

}
