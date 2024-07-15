package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.stream.Stream;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingStep;
import org.jooq.impl.DSL;

/**
 * Transformer for translating the intermediate representation of {@link QueryStep} into the final SQL query.
 */
public class QueryStepTransformer {

	private final DSLContext dslContext;

	public QueryStepTransformer(DSLContext dslContext) {
		this.dslContext = dslContext;
	}

	/**
	 * Converts a given {@link QueryStep} into an executable SELECT statement.
	 */
	public Select<Record> toSelectQuery(QueryStep queryStep) {

		SelectConditionStep<Record> queryBase = this.dslContext.with(constructPredecessorCteList(queryStep))
															   .select(queryStep.getSelects().all())
															   .from(queryStep.getFromTables())
															   .where(queryStep.getConditions());

		// grouping
		SelectHavingStep<Record> grouped = queryBase;
		if (queryStep.isGroupBy()) {
			grouped = queryBase.groupBy(queryStep.getGroupBy());
		}

		// union
		if (!queryStep.isUnion()) {
			return grouped;
		}
		return union(queryStep, grouped);
	}

	private List<CommonTableExpression<Record>> constructPredecessorCteList(QueryStep queryStep) {
		return queryStep.getPredecessors().stream()
						.flatMap(predecessor -> toCteList(predecessor).stream())
						.toList();
	}

	private List<CommonTableExpression<Record>> toCteList(QueryStep queryStep) {
		return Stream.concat(
				this.predecessorCtes(queryStep),
				Stream.of(toCte(queryStep))
		).toList();
	}

	private Stream<CommonTableExpression<Record>> predecessorCtes(QueryStep queryStep) {
		return queryStep.getPredecessors().stream()
						.flatMap(predecessor -> toCteList(predecessor).stream());
	}

	private CommonTableExpression<Record> toCte(QueryStep queryStep) {
		Select<Record> selectStep = toSelectStep(queryStep);
		return DSL.name(queryStep.getCteName()).as(selectStep);
	}

	private Select<Record> toSelectStep(QueryStep queryStep) {

		Select<Record> selectStep = this.dslContext
				.select(queryStep.getSelects().all())
				.from(queryStep.getFromTables())
				.where(queryStep.getConditions());

		if (queryStep.isGroupBy()) {
			selectStep = ((SelectConditionStep<Record>) selectStep).groupBy(queryStep.getGroupBy());
		}

		if (queryStep.isUnion()) {
			selectStep = union(queryStep, selectStep);
		}

		return selectStep;
	}

	private Select<Record> union(QueryStep queryStep, Select<Record> base) {
		for (QueryStep unionStep : queryStep.getUnion()) {
			base = queryStep.isUnionAll() ? base.unionAll(toSelectStep(unionStep)) : base.union(toSelectStep(unionStep));
		}
		return base;
	}

}
