package com.bakdata.conquery.sql.compiler;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import org.jooq.Field;

final class CompiledQueryColumns {

	private CompiledQueryColumns() {
	}

	static List<CompiledColumn> create(EntitySchema schema, List<ResultColumn> results, List<? extends Field<?>> fields) {
		if (fields.size() != 1 + results.size()) {
			throw new IllegalStateException("Compiled projection does not match the declared result columns");
		}
		List<CompiledColumn> columns = new ArrayList<>(fields.size());
		columns.add(new CompiledColumn(schema.primaryId().logicalId(), fields.getFirst().getName(),
				ResultType.Primitive.STRING, ColumnRole.ENTITY_ID));
		for (int index = 0; index < results.size(); index++) {
			ResultColumn result = results.get(index);
			columns.add(new CompiledColumn(result.outputId(), fields.get(index + 1).getName(), result.type(), ColumnRole.RESULT));
		}
		return columns;
	}
}
