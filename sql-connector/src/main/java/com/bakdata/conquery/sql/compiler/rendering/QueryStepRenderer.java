package com.bakdata.conquery.sql.compiler.rendering;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.ProjectionMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import lombok.RequiredArgsConstructor;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingStep;
import org.jooq.SelectSelectStep;
import org.jooq.impl.DSL;

/** Renders a tree of compiler query-step IR into a final jOOQ SELECT statement. */
@RequiredArgsConstructor
public class QueryStepRenderer {

	private final DSLContext dslContext;

	/**
	 * Render a final query step and all of its predecessor CTEs.
	 *
	 * @throws IllegalArgumentException when the supplied root step has an intermediate projection
	 */
	public Select<Record> toSelectQuery(QueryStep queryStep, CompilerDialect dialect) {

		List<Field<?>> finalRepresentation = SelectProjectionRenderer.renderFinal(
				queryStep.getSelects(),
				dialect,
				queryStep.getProjectionMode()
		);

		SelectConditionStep<Record> queryBase = this.dslContext.with(constructPredecessorCteList(queryStep, dialect))
				.select(finalRepresentation)
				.from(queryStep.getFromTables())
				.where(queryStep.getConditions());

		SelectHavingStep<Record> grouped = queryBase;
		if (queryStep.isGroupBy()) {
			grouped = queryBase.groupBy(queryStep.getGroupBy());
		}

		if (queryStep.isUnion()) {
			return union(queryStep, grouped, dialect);
		}

		return grouped;
	}

	private List<CommonTableExpression<Record>> constructPredecessorCteList(QueryStep queryStep, CompilerDialect dialect) {
		return predecessorCtes(queryStep, dialect).toList();
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
		List<Field<?>> allSelects = queryStep.getProjectionMode() == ProjectionMode.INTERMEDIATE
				? queryStep.getSelects().all()
				: SelectProjectionRenderer.renderFinal(queryStep.getSelects(), dialect, queryStep.getProjectionMode());

		SelectSelectStep<Record> selectClause = queryStep.isSelectDistinct()
				? this.dslContext.selectDistinct(allSelects)
				: this.dslContext.select(allSelects);

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
			// Final projections must use the same physical representation in every union branch.
			Select<Record> selectStep = queryStep.getProjectionMode() != ProjectionMode.INTERMEDIATE
					? toSelectQuery(unionStep, dialect)
					: toSelectStep(unionStep, dialect);

			base = queryStep.isUnionAll()
					? base.unionAll(selectStep)
					: base.union(selectStep);
		}
		return base;
	}
}
