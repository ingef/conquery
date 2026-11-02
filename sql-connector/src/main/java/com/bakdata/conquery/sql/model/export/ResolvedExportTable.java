package com.bakdata.conquery.sql.model.export;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;

/** One physical connector table participating in a row-level export. */
public record ResolvedExportTable(
		@NotBlank String conceptName,
		@NotBlank String connectorName,
		@NotNull @Valid SqlTable table,
		@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn primaryId,
		@NotNull @Valid ResolvedValidityDate validityDate,
		@NotBlank String source,
		@NotNull List<@NotNull Optional<@Valid ResolvedColumn>> outputColumns,
		@NotNull List<@NotNull @Valid ResolvedFilter> filters
) {
	public ResolvedExportTable {
		outputColumns = ModelNormalization.immutableCopy(outputColumns);
		filters = ModelNormalization.immutableCopy(filters);
	}

	@AssertTrue(message = "identifier column must belong to the export table")
	public boolean isPrimaryIdOnExportTable() {
		return table == null || primaryId == null || table.equals(primaryId.table());
	}

	@AssertTrue(message = "validity-date columns must belong to the export table")
	public boolean isValidityDateOnExportTable() {
		if (validityDate == null || table == null) {
			return true;
		}
		return switch (validityDate) {
			case ResolvedValidityDate.None ignored -> true;
			case ResolvedValidityDate.Point point -> point.column() == null || table.equals(point.column().table());
			case ResolvedValidityDate.Range range -> range.start() == null || range.end() == null
					|| table.equals(range.start().table()) && table.equals(range.end().table());
		};
	}

	@AssertTrue(message = "output columns must belong to the export table")
	public boolean isOutputColumnsOnExportTable() {
		return table == null || outputColumns == null || outputColumns.stream()
				.filter(java.util.Objects::nonNull)
				.flatMap(Optional::stream)
				.map(ResolvedColumn::table)
				.allMatch(table::equals);
	}
}
