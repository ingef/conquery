package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.stream.Stream;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
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
															   .from(queryStep.getFromTable())
															   .where(queryStep.getConditions());
		if (queryStep.isGroupBy()) {
			return queryBase.groupBy(queryStep.getGroupBy());
		}
		else {
			return queryBase;
		}
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

		Select<Record> selectStep = this.dslContext
				.select(queryStep.getSelects().all())
				.from(queryStep.getFromTable())
				.where(queryStep.getConditions());

		if (queryStep.isGroupBy()) {
			selectStep = ((SelectConditionStep<Record>) selectStep).groupBy(queryStep.getGroupBy());
		}

		if (queryStep.isUnion()) {
			for (QueryStep unionStep : queryStep.getUnion()) {
				// we only use the union as part of the date aggregation process - the entries of the UNION tables are all unique
				// thus we can use a UNION ALL because it's way faster than UNION
				selectStep = selectStep.unionAll(
						this.dslContext.select(unionStep.getSelects().all()).from(unionStep.getFromTable())
				);
			}
		}

		return DSL.name(queryStep.getCteName()).as(selectStep);
	}

}
