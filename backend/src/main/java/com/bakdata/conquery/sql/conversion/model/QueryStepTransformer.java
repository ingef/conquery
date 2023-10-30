package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.stream.Stream;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectGroupByStep;
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

		SelectGroupByStep<Record> where = this.dslContext.select(queryStep.getSelects().all())
														 .from(queryStep.getFromTable())
														 .where(queryStep.getConditions());

		ResultQuery<Record> query;
		if (queryStep.isGroupBy()) {
			query = where.groupBy(queryStep.getGroupBy());
		}
		else {
			query = where;
		}

		return DSL.name(queryStep.getCteName()).as(query);
	}

}
