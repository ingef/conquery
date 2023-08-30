package com.bakdata.conquery.sql.conversion.cqelement.concept.model;


import com.bakdata.conquery.models.datasets.concepts.select.Select;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jooq.Field;

@EqualsAndHashCode
public abstract class ConquerySelect {

	// An empty String as qualifier will omit the qualifying at all, but child classes can always call the method and have the ability to set a qualifier later.
	@Getter
	private String qualifier = "";

	public ConquerySelect qualify(String qualifier) {
		this.qualifier = qualifier;
		return this;
	}

	/**
	 * @return The whole (aliased) SQL expression for this {@link Select}.
	 */
	public abstract Field<?> select();

	/**
	 * @return Plain column name (alias) that can be used to reference the created select.
	 */
	public abstract Field<?> alias();

}
