package com.bakdata.conquery.sql.compiler.ir.select;

import java.util.List;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.Qualifiable;
import org.jooq.Field;

/** Intermediate representation of one or more SQL select expressions. */
public interface SqlSelect extends Qualifiable<SqlSelect> {

	List<Field<?>> toFields();

	/** All column names this select requires. */
	List<String> requiredColumns();

	/** Whether this select only occurs in the final concept CTE and has no preceding select. */
	default boolean isUniversal() {
		return false;
	}

	/** Apply an extra aggregation required when joining this select across connectors. */
	default SqlSelect connectorAggregate() {
		return this;
	}

	/** Convert this select into the representation returned by the final SQL query. */
	default SqlSelect toFinalRepresentation() {
		return this;
	}

	/** Aggregate this select to one value per ID group in the final concept query. */
	default List<Field<?>> aggregateForFinalQuery(CompilerDialect dialect) {
		return toFinalRepresentation().toFields().stream()
				.<Field<?>>map(field -> dialect.anyValue(field).as(field.getName()))
				.toList();
	}
}
