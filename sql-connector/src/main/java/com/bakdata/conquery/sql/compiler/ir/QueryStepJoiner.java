package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.table;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;

/** Combines compiler query-step IR without applying backend query semantics. */
public final class QueryStepJoiner {

	private QueryStepJoiner() {
	}

	/**
	 * Join the CTE tables represented by the supplied steps on their IDs and, where present, stratification dates.
	 */
	public static TableLike<Record> join(List<QueryStep> querySteps, JoinMode joinMode) {
		if (querySteps.isEmpty()) {
			throw new IllegalArgumentException("Need at least one query step to construct a join");
		}

		Table<Record> joinedTable = table(name(querySteps.getFirst().getCteName()));
		for (int index = 0; index < querySteps.size() - 1; index++) {
			QueryStep leftStep = querySteps.get(index);
			QueryStep rightStep = querySteps.get(index + 1);

			List<Condition> joinConditions = Stream.concat(
					leftStep.getQualifiedSelects().getIds().join(rightStep.getQualifiedSelects().getIds()).stream(),
					Stream.of(joinOnStratification(leftStep, rightStep))
			).toList();

			Table<Record> rightTable = table(name(rightStep.getCteName()));
			joinedTable = join(joinedTable, rightTable, joinConditions, joinMode);
		}

		return joinedTable;
	}

	/** Merge the explicitly selected fields from the supplied query steps. */
	public static List<SqlSelect> mergeSelects(List<QueryStep> querySteps) {
		return querySteps.stream()
				.flatMap(queryStep -> queryStep.getQualifiedSelects().getSqlSelects().stream())
				.toList();
	}

	/** Coalesce the entity IDs selected by the supplied query steps. */
	public static SqlIdColumns coalesceIds(List<QueryStep> querySteps) {
		List<SqlIdColumns> ids = querySteps.stream()
				.map(QueryStep::getQualifiedSelects)
				.map(Selects::getIds)
				.toList();
		if (ids.isEmpty()) {
			throw new IllegalArgumentException("Need at least one query step to coalesce IDs");
		}
		return ids.getFirst().coalesce(ids.subList(1, ids.size()));
	}

	private static TableOnConditionStep<Record> join(
			Table<?> left,
			Table<?> right,
			List<Condition> conditions,
			JoinMode joinMode
	) {
		Condition[] conditionArray = conditions.toArray(Condition[]::new);
		return switch (joinMode) {
			case INNER -> left.innerJoin(right).on(conditionArray);
			case FULL_OUTER -> left.fullOuterJoin(right).on(conditionArray);
			case LEFT -> left.leftJoin(right).on(conditionArray);
		};
	}

	private static Condition joinOnStratification(QueryStep leftStep, QueryStep rightStep) {
		Optional<ColumnDateRange> leftDate = leftStep.getQualifiedSelects().getStratificationDate();
		Optional<ColumnDateRange> rightDate = rightStep.getQualifiedSelects().getStratificationDate();
		if (leftDate.isEmpty() || rightDate.isEmpty()) {
			return noCondition();
		}
		return leftDate.get().join(rightDate.get());
	}
}
