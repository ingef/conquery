package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FieldSelect extends ConquerySelect {

	private final Field<?> field;

	@Override
	public Field<?> select() {
		return field;
	}

	@Override
	public Field<?> alias() {
		return DSL.field(field.getName());
	}

}
