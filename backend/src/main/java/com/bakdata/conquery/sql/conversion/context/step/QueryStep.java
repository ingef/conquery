package com.bakdata.conquery.sql.conversion.context.step;

import java.util.List;

import com.bakdata.conquery.sql.conversion.context.selects.Selects;
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
	List<Condition> conditions;
	/**
	 * The CTEs referenced by this QueryStep
	 */
	List<QueryStep> predecessors;

	public static TableLike<Record> toTableLike(String fromTableName) {
		return DSL.table(DSL.name(fromTableName));
	}

	public Selects getSelects() {
		return this.selects;
	}

	/**
	 * @return All selects re-mapped to a qualifier, which is the cteName of this QueryStep.
	 */
	public Selects getQualifiedSelects() {
		return this.selects.byName(this.cteName);
	}

}
