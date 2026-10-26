package com.bakdata.conquery.sql.model.export;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.result.ResultColumn;

/** Fully resolved input for exporting individual physical rows. */
public record ResolvedTableExportQuery(
		@NotNull @Valid ResolvedQuery prerequisite,
		@NotNull @Valid DateRange dateRestriction,
		@NotEmpty List<@NotNull @Valid ResolvedExportTable> tables,
		@NotNull List<@NotNull @Valid ResultColumn> resultColumns
) {
	public ResolvedTableExportQuery {
		tables = ModelNormalization.immutableCopy(tables);
		resultColumns = ModelNormalization.immutableCopy(resultColumns);
	}

	@AssertTrue(message = "result column outputIds must be unique")
	public boolean isResultColumnOutputIdsUnique() {
		return resultColumns == null || resultColumns.stream()
				.map(ResultColumn::outputId)
				.distinct()
				.count() == resultColumns.size();
	}
}
