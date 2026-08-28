package com.bakdata.conquery.sql.query.operation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.query.internal.ModelNormalization;
import com.bakdata.conquery.sql.query.range.NumberRange;
import com.bakdata.conquery.sql.query.range.SubstringRange;
import com.bakdata.conquery.sql.query.schema.ResolvedColumn;
import com.bakdata.conquery.sql.query.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Framework-neutral filter operations supported by the SQL connector. */
public final class BuiltInFilters {

	private BuiltInFilters() {
	}

	public record StringValues(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn column,
			@NotEmpty Set<@NotNull String> values,
			@NotNull Optional<@Valid SubstringRange> substring
	) implements ResolvedFilter {

		public StringValues {
			values = ModelNormalization.immutableCopy(values);
		}

		public StringValues(ResolvedColumn column, Set<String> values) {
			this(column, values, Optional.empty());
		}
	}

	public record NumericColumnRange(
			@NotNull @Valid @AllowedColumnTypes({ColumnType.INTEGER, ColumnType.REAL, ColumnType.DECIMAL, ColumnType.MONEY})
			ResolvedColumn column,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	public record AggregationRange(
			@NotNull @Valid ResolvedAggregation aggregation,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	/** Filters by distance to an end date that was frozen while resolving the query. */
	public record DateDistanceRange(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.DATE) ResolvedColumn column,
			@NotNull ChronoUnit unit,
			@NotNull LocalDate endDate,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	public record Flags(
			@NotNull @Valid BuiltInAggregations.Flags availableFlags,
			@NotEmpty Set<@NotNull String> selectedFlags
	) implements ResolvedFilter {

		public Flags {
			selectedFlags = ModelNormalization.immutableCopy(selectedFlags);
		}

		public Flags(java.util.Map<String, ResolvedColumn> availableFlags, Set<String> selectedFlags) {
			this(new BuiltInAggregations.Flags(availableFlags), selectedFlags);
		}

		@AssertTrue(message = "selectedFlags must be defined by availableFlags")
		public boolean isSelectionKnown() {
			return availableFlags == null || availableFlags.columns() == null || selectedFlags == null
					|| availableFlags.columns().keySet().containsAll(selectedFlags);
		}
	}
}
