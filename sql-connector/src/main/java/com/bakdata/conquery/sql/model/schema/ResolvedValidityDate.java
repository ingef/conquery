package com.bakdata.conquery.sql.model.schema;

import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;

/** Physical representation of event validity for a connector. */
public sealed interface ResolvedValidityDate permits ResolvedValidityDate.None, ResolvedValidityDate.Point, ResolvedValidityDate.Range {

	record None() implements ResolvedValidityDate {
	}

	record Point(
			@NotNull @Valid @AllowedColumnTypes(MajorTypeId.DATE) ResolvedColumn column
	) implements ResolvedValidityDate {
	}

	record Range(
			@NotNull @Valid @AllowedColumnTypes(MajorTypeId.DATE) ResolvedColumn start,
			@NotNull @Valid @AllowedColumnTypes(MajorTypeId.DATE) ResolvedColumn end
	) implements ResolvedValidityDate {

		@AssertTrue(message = "validity-date columns must belong to the same table")
		public boolean isOnSameTable() {
			return start == null || end == null || Objects.equals(start.table(), end.table());
		}
	}
}
