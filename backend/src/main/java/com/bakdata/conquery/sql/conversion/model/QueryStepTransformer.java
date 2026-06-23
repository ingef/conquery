package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
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
	public Select<Record> toSelectQuery(QueryStep queryStep, SqlFunctionProvider functionProvider) {

		List<Field<?>> finalRepresentation = queryStep.getSelects().toFinalRepresentation(functionProvider, queryStep.isForTableExport());

		SelectConditionStep<Record> queryBase = this.dslContext.with(constructPredecessorCteList(queryStep, functionProvider))
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
			return union(queryStep, grouped, functionProvider);
		}

		return grouped;
	}

	private List<CommonTableExpression<Record>> constructPredecessorCteList(QueryStep queryStep, SqlFunctionProvider functionProvider) {
		return predecessorCtes(queryStep, functionProvider)
				.toList();
	}

	private List<CommonTableExpression<Record>> toCteList(QueryStep queryStep, SqlFunctionProvider functionProvider) {
		return Stream.concat(
				this.predecessorCtes(queryStep, functionProvider),
				Stream.of(toCte(queryStep, functionProvider))
		).toList();
	}

	private Stream<CommonTableExpression<Record>> predecessorCtes(QueryStep queryStep, SqlFunctionProvider functionProvider) {
		return queryStep.getPredecessors().stream()
				.flatMap(predecessor -> toCteList(predecessor, functionProvider).stream());
	}

	private CommonTableExpression<Record> toCte(QueryStep queryStep, SqlFunctionProvider functionProvider) {
		Select<Record> selectStep = toSelectStep(queryStep, functionProvider);
		return DSL.name(queryStep.getCteName()).as(selectStep);
	}

	private Select<Record> toSelectStep(QueryStep queryStep, SqlFunctionProvider functionProvider) {

		SelectSelectStep<Record> selectClause;

		List<Field<?>> allSelects = queryStep.isForTableExport() ? queryStep.getSelects().toFinalRepresentation(functionProvider, true) : queryStep.getSelects().all();

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
			selectStep = union(queryStep, selectStep, functionProvider);
		}

		return selectStep;
	}

	private Select<Record> union(QueryStep queryStep, Select<Record> base, SqlFunctionProvider functionProvider) {
		for (QueryStep unionStep : queryStep.getUnion()) {
			Select<Record> selectStep =
					queryStep.isForTableExport() ?
							// TODO this feels like a leaked abstraction, but i am not able to find the proper injection layer at the moment.
							toSelectQuery(unionStep.toBuilder().forTableExport(true).build(), functionProvider) :
							toSelectStep(unionStep, functionProvider);

			if (queryStep.isUnionAll()) {
				base = base.unionAll(selectStep);
			} else {
				base = base.union(selectStep);
			}
		}
		return base;
	}

}
