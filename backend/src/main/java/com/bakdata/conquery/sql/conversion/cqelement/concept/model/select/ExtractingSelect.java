package com.bakdata.conquery.sql.conversion.cqelement.concept.model.select;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Select that does nothing but reference an existing column.
 * <p>
 * This can be used if another select requires a column in a later step.
 *
 * @param <V> type of column
 */
@Value
@EqualsAndHashCode
public class ExtractingSelect<V> implements ConquerySelect {

	String table;
	String column;
	@EqualsAndHashCode.Exclude
	Class<V> columnClass;

	@SuppressWarnings("unchecked")
	public static <V> ExtractingSelect<V> fromConquerySelect(ConquerySelect select, String qualifier) {
		return (ExtractingSelect<V>) new ExtractingSelect<>(
				qualifier,
				select.columnName(),
				select.aliased().getType()
		);
	}

	@Override
	public Field<V> select() {
		return DSL.field(DSL.name(table, column), columnClass);
	}

	@Override
	public Field<V> aliased() {
		return DSL.field(column, columnClass);
	}

	@Override
	public String columnName() {
		return column;
	}

}
