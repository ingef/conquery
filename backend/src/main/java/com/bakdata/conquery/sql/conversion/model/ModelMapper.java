package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import lombok.experimental.UtilityClass;

/**
 * Converts legacy backend schema objects into the connector's resolved schema model.
 *
 */
@UtilityClass
public class ModelMapper {

	public static EntitySchema map(IdColumnConfig idColumns) {
		ColumnConfig primaryId = idColumns.findPrimaryIdColumn();
		SqlTable entityTable = SqlTable.of(idColumns.getTable(), idColumns.getTable());
		ResolvedColumn primaryIdColumn = new ResolvedColumn(
				primaryId.getName(),
				entityTable,
				primaryId.getField(),
				MajorTypeId.STRING,
				false
		);
		return new EntitySchema(primaryIdColumn);
	}

	public static SqlTable map(Table table) {
		return SqlTable.of(table.getId().toString(), table.getName());
	}

	public static ResolvedColumn map(Column column) {
		return new ResolvedColumn(column.getId().toString(), map(column.getTable()), column.getName(),
				MajorTypeId.valueOf(column.getType().name()), true);
	}

	public static DateColumns map(DaterangeSelectOrFilter dates) {
		return dates.isSingleColumnDaterange()
				? new DateColumns.Single(map(dates.getColumn().resolve()))
				: new DateColumns.Pair(map(dates.getStartColumn().resolve()), map(dates.getEndColumn().resolve()));
	}
}
