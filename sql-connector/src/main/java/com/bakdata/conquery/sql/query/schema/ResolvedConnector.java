package com.bakdata.conquery.sql.query.schema;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.query.internal.ModelNormalization;
import com.bakdata.conquery.sql.query.operation.ResolvedCondition;
import com.bakdata.conquery.sql.query.operation.ResolvedFilter;
import com.bakdata.conquery.sql.query.operation.ResolvedSelect;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A selected connector with all physical columns and operations resolved. */
public record ResolvedConnector(
		@NotBlank String logicalId,
		@NotNull @Valid SqlTable table,
		@NotNull @Valid ResolvedColumn primaryId,
		@NotNull Optional<@Valid ResolvedColumn> secondaryId,
		@NotNull @Valid ResolvedValidityDate validityDate,
		@NotNull List<@NotNull @Valid ResolvedFilter> filters,
		@NotNull List<@NotNull @Valid ResolvedSelect> selects,
		@NotNull List<@NotNull @Valid ResolvedCondition> conditions
) {

	public ResolvedConnector {
		filters = ModelNormalization.immutableCopy(filters);
		selects = ModelNormalization.immutableCopy(selects);
		conditions = ModelNormalization.immutableCopy(conditions);
	}

	@AssertTrue(message = "identifier columns must belong to the connector table")
	public boolean isIdentifierColumnsOnConnectorTable() {
		return table == null
				|| primaryId == null
				|| secondaryId == null
				|| table.equals(primaryId.table()) && secondaryId.stream().map(ResolvedColumn::table).allMatch(table::equals);
	}

	@AssertTrue(message = "validity-date columns must belong to the connector table")
	public boolean isValidityDateOnConnectorTable() {
		if (validityDate == null || table == null) {
			return true;
		}
		switch (validityDate) {
			case ResolvedValidityDate.None ignored -> {
				return true;
			}
			case ResolvedValidityDate.Point point -> {
				return point.column() == null || table.equals(point.column().table());
			}
			case ResolvedValidityDate.Range range -> {
				return range.start() == null || range.end() == null
						|| table.equals(range.start().table()) && table.equals(range.end().table());
			}
		}
	}
}
