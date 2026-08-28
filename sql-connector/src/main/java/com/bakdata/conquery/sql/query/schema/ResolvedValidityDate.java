package com.bakdata.conquery.sql.query.schema;

import java.util.Objects;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.query.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Physical representation of event validity for a connector. */
public sealed interface ResolvedValidityDate permits ResolvedValidityDate.None, ResolvedValidityDate.Point, ResolvedValidityDate.Range {

	record None() implements ResolvedValidityDate {
	}

	record Point(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn column
	) implements ResolvedValidityDate {
	}

	record Range(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn start,
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn end
	) implements ResolvedValidityDate {

		@AssertTrue(message = "validity-date columns must belong to the same table")
		public boolean isOnSameTable() {
			return start == null || end == null || Objects.equals(start.table(), end.table());
		}
	}
}
