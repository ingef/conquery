package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
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
	TableLike<Record> fromTable;
	@Builder.Default
	List<Condition> conditions = Collections.emptyList();
	@Builder.Default
	List<Field<?>> groupBy = Collections.emptyList();
	/**
	 * All {@link QueryStep}'s that shall be converted before this {@link QueryStep}.
	 */
	@Builder.Default
	List<QueryStep> predecessors = Collections.emptyList();

	public static TableLike<Record> toTableLike(String fromTableName) {
		return DSL.table(DSL.name(fromTableName));
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

}
