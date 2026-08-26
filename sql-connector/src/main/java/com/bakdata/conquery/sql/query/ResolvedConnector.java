package com.bakdata.conquery.sql.query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A selected connector with all physical columns and operations resolved. */
public record ResolvedConnector(
		String logicalId,
		SqlTable table,
		ResolvedColumn primaryId,
		Optional<ResolvedColumn> secondaryId,
		ResolvedValidityDate validityDate,
		List<ResolvedFilter> filters,
		List<ResolvedSelect> selects,
		List<ResolvedCondition> conditions
) {

	public ResolvedConnector {
		logicalId = ModelValidation.requireNonBlank(logicalId, "logicalId");
		Objects.requireNonNull(table, "table");
		requireColumnOnTable(primaryId, table, "primaryId");
		if (Objects.requireNonNull(secondaryId, "secondaryId").isPresent()) {
			secondaryId = Optional.of(requireColumnOnTable(secondaryId.get(), table, "secondaryId"));
		}
		Objects.requireNonNull(validityDate, "validityDate");
		validateValidityDateTable(validityDate, table);
		filters = List.copyOf(Objects.requireNonNull(filters, "filters"));
		selects = List.copyOf(Objects.requireNonNull(selects, "selects"));
		conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
	}

	private static ResolvedColumn requireColumnOnTable(ResolvedColumn column, SqlTable table, String name) {
		Objects.requireNonNull(column, name);
		if (!table.equals(column.table())) {
			throw new IllegalArgumentException(name + " must belong to the connector table");
		}
		return column;
	}

	private static void validateValidityDateTable(ResolvedValidityDate validityDate, SqlTable table) {
		switch (validityDate) {
			case ResolvedValidityDate.None ignored -> {
			}
			case ResolvedValidityDate.Point point -> requireColumnOnTable(point.column(), table, "validityDate");
			case ResolvedValidityDate.Range range -> {
				requireColumnOnTable(range.start(), table, "validityDate.start");
				requireColumnOnTable(range.end(), table, "validityDate.end");
			}
		}
	}
}
