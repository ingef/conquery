package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/**
 * Intermediate representation of an SQL query.
 */
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
	 * All {@link Field}s that should be part of the SQL GROUPY BY clause.
	 */
	@Builder.Default
	List<Field<?>> groupBy = Collections.emptyList();
	/**
	 * All {@link QueryStep}s that should be connected via a SQL UNION operator
	 */
	@Builder.Default
	List<QueryStep> union = Collections.emptyList();
	/**
	 * All {@link QueryStep}'s that shall be converted before this {@link QueryStep}.
	 */
	@Singular
	List<QueryStep> predecessors;

	public static QueryStep createUnionStep(List<QueryStep> unionSteps, String cteName, List<QueryStep> predecessors) {
		return unionSteps.get(0)
						 .toBuilder()
						 .cteName(cteName)
						 .union(unionSteps.subList(1, unionSteps.size()))
						 .predecessors(predecessors)
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
	 * @return All selects re-mapped to a qualifier, which is the cteName of this QueryStep.
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
