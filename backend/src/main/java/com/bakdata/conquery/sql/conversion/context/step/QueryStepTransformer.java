package com.bakdata.conquery.sql.conversion.context.step;

import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.stream.Stream;

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
		return this.dslContext.with(this.constructPredecessorCteList(queryStep))
							  .select(queryStep.getSelects().all())
							  .from(queryStep.getFromTable())
							  .where(queryStep.getConditions());
	}

	private List<CommonTableExpression<Record>> constructPredecessorCteList(QueryStep queryStep) {
		return queryStep.getPredecessors().stream()
						.flatMap(predecessor -> this.toCteList(predecessor).stream())
						.toList();
	}

	private List<CommonTableExpression<Record>> toCteList(QueryStep queryStep) {
		return Stream.concat(
				this.predecessorCtes(queryStep),
				Stream.of(this.toCte(queryStep))
		).toList();
	}

	private Stream<CommonTableExpression<Record>> predecessorCtes(QueryStep queryStep) {
		return queryStep.getPredecessors().stream()
						.flatMap(predecessor -> this.toCteList(predecessor).stream());
	}

	private CommonTableExpression<Record> toCte(QueryStep queryStep) {

		SelectGroupByStep<Record> where = this.dslContext.select(queryStep.getSelects().all())
														 .from(queryStep.getFromTable())
														 .where(queryStep.getConditions());

		ResultQuery<Record> query;
		if (queryStep.isGroupBy()) {
			query = where.groupBy(queryStep.getSelects().getPrimaryColumn());
		}
		else {
			query = where;
		}

		return DSL.name(queryStep.getCteName()).as(query);
	}

}
