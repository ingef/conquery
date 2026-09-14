package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;

/** Converts the legacy entity-ID configuration into the connector's resolved entity schema. */
public final class EntitySchemaAdapter {

	private EntitySchemaAdapter() {
	}

	public static EntitySchema from(IdColumnConfig idColumns) {
		ColumnConfig primaryId = idColumns.findPrimaryIdColumn();
		SqlTable entityTable = SqlTable.of(idColumns.getTable(), idColumns.getTable());
		ResolvedColumn primaryIdColumn = new ResolvedColumn(
				primaryId.getName(),
				entityTable,
				primaryId.getField(),
				ColumnType.STRING,
				false
		);
		return new EntitySchema(entityTable, primaryIdColumn);
	}
}
