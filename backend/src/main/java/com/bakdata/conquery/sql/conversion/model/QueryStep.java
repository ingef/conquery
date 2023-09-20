package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/**
 * Intermediate representation of an SQL query.
 */
@Value
@Builder
public class QueryStep {

	String cteName;
	Selects selects;
	TableLike<Record> fromTable;
	@Builder.Default
	List<Condition> conditions = Collections.emptyList();
	/**
	 * The CTEs referenced by this QueryStep
	 */
	List<QueryStep> predecessors;
	@Builder.Default
	boolean isGroupBy = false;

	public static TableLike<Record> toTableLike(String fromTableName) {
		return DSL.table(DSL.name(fromTableName));
	}

	/**
	 * @return All selects re-mapped to a qualifier, which is the cteName of this QueryStep.
	 */
	public Selects getQualifiedSelects() {
		return this.selects.qualifiedWith(this.cteName);
	}

}
