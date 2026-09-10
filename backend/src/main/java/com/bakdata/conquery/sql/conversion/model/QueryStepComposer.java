package com.bakdata.conquery.sql.conversion.model;

import java.util.List;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;

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
				toEntitySchema(context.getIdColumns()),
				dateAggregationAction,
				context.getCompilerDialect()
		);
	}

	public static QueryStep joinChildren(
			Iterable<CQElement> children,
			ConversionContext context,
			JoinMode logicalOperation,
			DateAggregationAction dateAggregationAction
	) {
		ConversionContext childrenContext = context.createChildContext();
		for (CQElement childNode : children) {
			childrenContext = context.getNodeConversions().convert(childNode, childrenContext);
		}
		return joinSteps(childrenContext.getQuerySteps(), logicalOperation, dateAggregationAction, context);
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
				toEntitySchema(context.getIdColumns()),
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
	}

	private static EntitySchema toEntitySchema(IdColumnConfig idColumns) {
		ColumnConfig primaryId = idColumns.findPrimaryIdColumn();
		SqlTable entityTable = SqlTable.of(idColumns.getTable(), idColumns.getTable());
		ResolvedColumn primaryIdColumn = new ResolvedColumn(
				primaryId.getName(),
				entityTable,
				primaryId.getField(),
				ColumnType.STRING,
				false
		);
		return new EntitySchema(entityTable, primaryIdColumn);
	}
}
