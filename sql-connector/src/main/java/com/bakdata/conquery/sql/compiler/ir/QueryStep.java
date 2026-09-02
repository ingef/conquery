package com.bakdata.conquery.sql.compiler.ir;

import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

import java.util.Collections;
import java.util.List;

/** Intermediate representation of one SQL query step. */
@Value
@Builder(toBuilder = true)
public class QueryStep {

	String cteName;
	Selects selects;
	@Singular
	List<TableLike<? extends Record>> fromTables;
	@Builder.Default
	List<Condition> conditions = Collections.emptyList();
	/**
	 * All {@link Field}s that should be part of the SQL GROUP BY clause.
	 */
	@Builder.Default
	List<Field<?>> groupBy = Collections.emptyList();
	/**
	 * All {@link QueryStep}s that should be connected via a SQL UNION operator.
	 */
	@Builder.Default
	List<QueryStep> union = Collections.emptyList();
	/**
	 * Determines whether this step's union steps should be unioned using UNION ALL. Defaults to {@code true}.
	 */
	@Builder.Default
	boolean unionAll = true;

	@Builder.Default
	ProjectionMode projectionMode = ProjectionMode.INTERMEDIATE;

	/** Whether the query should be negated. */
	@Builder.Default
	boolean negate = false;
	/** Whether the SELECT should be distinct. */
	boolean selectDistinct;
	/** All {@link QueryStep}s that must be converted before this step. */
	@Singular
	List<QueryStep> predecessors;

	public static QueryStep createUnionAllStep(List<QueryStep> unionSteps, String cteName, List<QueryStep> predecessors, boolean negation) {
		return createUnionStep(unionSteps, cteName, predecessors, true, negation);
	}

	public static QueryStep createUnionStep(List<QueryStep> unionSteps, String cteName, List<QueryStep> predecessors, boolean negation) {
		return createUnionStep(unionSteps, cteName, predecessors, false, negation);
	}

	private static QueryStep createUnionStep(List<QueryStep> unionSteps, String cteName, List<QueryStep> predecessors, boolean unionAll, boolean negation) {
		QueryStep first = unionSteps.getFirst();
		return first.toBuilder()
				.cteName(cteName)
				.union(unionSteps.stream().skip(1).toList())
				.unionAll(unionAll)
				.predecessors(predecessors)
				.negate(negation)
				.build();
	}

	public static TableLike<Record> toTableLike(String fromTableName) {
		return DSL.table(DSL.name(fromTableName));
	}

	public QueryStep addSqlSelect(SqlSelect sqlSelect) {
		Selects withAdditionalSelect = this.selects.toBuilder().sqlSelect(sqlSelect).build();
		return this.toBuilder().selects(withAdditionalSelect).build();
	}

	/**
	 * @return all selects qualified by this step's CTE name
	 */
	public Selects getQualifiedSelects() {
		return this.selects.qualify(this.cteName);
	}

	public boolean isGroupBy() {
		return !this.groupBy.isEmpty();
	}

	public boolean isUnion() {
		return !this.union.isEmpty();
	}
}
