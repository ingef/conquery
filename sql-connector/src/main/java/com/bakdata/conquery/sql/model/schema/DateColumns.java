package com.bakdata.conquery.sql.model.schema;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Resolved physical representation of a date or date range used by an operation. */
public sealed interface DateColumns permits DateColumns.Single, DateColumns.Pair {

	List<ResolvedColumn> columns();

	record Single(
			@NotNull @Valid @AllowedColumnTypes({ColumnType.DATE, ColumnType.DATE_RANGE}) ResolvedColumn column
	) implements DateColumns {

		@Override
		public List<ResolvedColumn> columns() {
			return column == null ? List.of() : List.of(column);
		}
	}

	record Pair(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn start,
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn end
	) implements DateColumns {

		@Override
		public List<ResolvedColumn> columns() {
			return start == null || end == null ? List.of() : List.of(start, end);
		}

		@AssertTrue(message = "date columns must belong to the same table")
		public boolean isOnSameTable() {
			return start == null || end == null || Objects.equals(start.table(), end.table());
		}
	}
}
