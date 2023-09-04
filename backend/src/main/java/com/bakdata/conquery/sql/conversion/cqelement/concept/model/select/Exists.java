package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode
public class Exists implements ConquerySelect {

	private final String label;

	@Override
	public Field<?> select() {
		return DSL.field("1", Integer.class)
				  .as(label + "_exists");
	}

	@Override
	public Field<?> alias() {
		return DSL.field(label + "_exists");
	}

}
