package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.rendering.SelectProjectionRenderer;
import com.bakdata.conquery.sql.compiler.rendering.SelectProjectionRenderer.ValidityDateRendering;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.stream.Stream;

/**
 * Transformer for translating the intermediate representation of {@link QueryStep} into the final SQL query.
 */
@RequiredArgsConstructor
public class QueryStepTransformer {

	private final DSLContext dslContext;

	/**
	 * Converts a given {@link QueryStep} into an executable SELECT statement.
	 */
	public Select<Record> toSelectQuery(QueryStep queryStep, CompilerDialect dialect) {

		ValidityDateRendering validityDateRendering = queryStep.isForTableExport()
				? ValidityDateRendering.INDIVIDUAL
				: ValidityDateRendering.AGGREGATED;
		List<Field<?>> finalRepresentation = SelectProjectionRenderer.render(queryStep.getSelects(), dialect, validityDateRendering);

		SelectConditionStep<Record> queryBase = this.dslContext.with(constructPredecessorCteList(queryStep, dialect))
				.select(finalRepresentation)
				.from(queryStep.getFromTables())
				.where(queryStep.getConditions());

		// grouping
		SelectHavingStep<Record> grouped = queryBase;
		if (queryStep.isGroupBy()) {
			grouped = queryBase.groupBy(queryStep.getGroupBy());
		}

		// union
		if (queryStep.isUnion()) {
			return union(queryStep, grouped, dialect);
		}

		return grouped;
	}

	private List<CommonTableExpression<Record>> constructPredecessorCteList(QueryStep queryStep, CompilerDialect dialect) {
		return predecessorCtes(queryStep, dialect)
				.toList();
	}

	private List<CommonTableExpression<Record>> toCteList(QueryStep queryStep, CompilerDialect dialect) {
		return Stream.concat(
				this.predecessorCtes(queryStep, dialect),
				Stream.of(toCte(queryStep, dialect))
		).toList();
	}

	private Stream<CommonTableExpression<Record>> predecessorCtes(QueryStep queryStep, CompilerDialect dialect) {
		return queryStep.getPredecessors().stream()
				.flatMap(predecessor -> toCteList(predecessor, dialect).stream());
	}

	private CommonTableExpression<Record> toCte(QueryStep queryStep, CompilerDialect dialect) {
		Select<Record> selectStep = toSelectStep(queryStep, dialect);
		return DSL.name(queryStep.getCteName()).as(selectStep);
	}

	private Select<Record> toSelectStep(QueryStep queryStep, CompilerDialect dialect) {

		SelectSelectStep<Record> selectClause;

		List<Field<?>> allSelects = queryStep.isForTableExport()
				? SelectProjectionRenderer.render(queryStep.getSelects(), dialect, ValidityDateRendering.INDIVIDUAL)
				: queryStep.getSelects().all();

		if (queryStep.isSelectDistinct()) {
			selectClause = dslContext.selectDistinct(allSelects);
		} else {
			selectClause = dslContext.select(allSelects);
		}

		Select<Record> selectStep = selectClause.from(queryStep.getFromTables()).where(queryStep.getConditions());

		if (queryStep.isGroupBy()) {
			selectStep = ((SelectConditionStep<Record>) selectStep).groupBy(queryStep.getGroupBy());
		}

		if (queryStep.isUnion()) {
			selectStep = union(queryStep, selectStep, dialect);
		}

		return selectStep;
	}

	private Select<Record> union(QueryStep queryStep, Select<Record> base, CompilerDialect dialect) {
		for (QueryStep unionStep : queryStep.getUnion()) {
			Select<Record> selectStep =
					queryStep.isForTableExport() ?
							// TODO this feels like a leaked abstraction, but i am not able to find the proper injection layer at the moment.
							toSelectQuery(unionStep, dialect) :
							toSelectStep(unionStep, dialect);

			if (queryStep.isUnionAll()) {
				base = base.unionAll(selectStep);
			} else {
				base = base.union(selectStep);
			}
		}
		return base;
	}

}
