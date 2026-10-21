package com.bakdata.conquery.sql.translate.conversion.select;

import static org.jooq.impl.DSL.*;

import java.util.Optional;

import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SingleColumnSqlSelect;
import org.jooq.Field;

/** SQL portion of the backend's mappable single-column select. */
public final class SubstringSelect {
	private SubstringSelect() {
	}

	public static SingleColumnSqlSelect getSubstringSelect(ResolvedColumn column, Optional<SubstringRange> substringRange, String rootTable, String alias) {
		Field<String> field = field(name(rootTable, column.physicalName()), String.class);
		if (substringRange.isPresent()) {
			SubstringRange range = substringRange.get();
			if (range.endExclusive().isEmpty()) {
				field = substring(field, 1 + range.startInclusive());
			}
			else {
				field = substring(field, 1 + range.startInclusive(), range.endExclusive().get() - range.startInclusive());
			}
		}
		if (alias != null) {
			field = field.as(name(alias));
		}
		return new FieldWrapper<>(field, column.physicalName());
	}
}
