package com.bakdata.conquery.sql.conversion.cqelement.concept.model;


import com.bakdata.conquery.models.datasets.concepts.select.Select;
import org.jooq.Field;

public interface ConquerySelect {

	/**
	 * @return The whole (aliased) SQL expression for this {@link Select}.
	 */
	Field<?> select();

	/**
	 * @return Plain column name (alias) that can be used to reference the created select.
	 */
	Field<?> alias();

}
