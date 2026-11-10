package com.bakdata.conquery.sql.model.operation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.operation.aggregation.FlagsAggregation;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;
import lombok.experimental.UtilityClass;

/** Framework-neutral filter operations supported by the SQL connector. */
@UtilityClass
public final class BuiltInFilters {

	public record StringValues(
			@NotBlank String name,
			@NotNull @Valid @AllowedColumnTypes(MajorTypeId.STRING) ResolvedColumn column,
			@NotEmpty Set<@NotNull String> values,
			@NotNull Optional<@Valid SubstringRange> substring
	) implements ResolvedFilter {

		public StringValues {
			values = ModelNormalization.immutableCopy(values);
		}

		public StringValues(String name, ResolvedColumn column, Set<String> values) {
			this(name, column, values, Optional.empty());
		}
	}

	public record NumericColumnRange(
			@NotBlank String name,
			@NotNull @Valid @AllowedColumnTypes({MajorTypeId.INTEGER, MajorTypeId.REAL, MajorTypeId.DECIMAL, MajorTypeId.MONEY})
			ResolvedColumn column,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	public record AggregationRange(
			@NotBlank String name,
			@NotNull @Valid ResolvedAggregation aggregation,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	/** Filters by distance to an end date that was frozen while resolving the query. */
	public record DateDistanceRange(
			@NotBlank String name,
			@NotNull @Valid @AllowedColumnTypes(MajorTypeId.DATE) ResolvedColumn column,
			@NotNull ChronoUnit unit,
			@NotNull LocalDate endDate,
			@NotNull @Valid NumberRange range
	) implements ResolvedFilter {
	}

	public record Flags(
			@NotBlank String name,
			@NotNull @Valid FlagsAggregation availableFlags,
			@NotEmpty Set<@NotNull String> selectedFlags
	) implements ResolvedFilter {

		public Flags {
			selectedFlags = ModelNormalization.immutableCopy(selectedFlags);
		}

		public Flags(String name, java.util.Map<String, ResolvedColumn> availableFlags, Set<String> selectedFlags) {
			this(name, new FlagsAggregation(availableFlags), selectedFlags);
		}

		@AssertTrue(message = "selectedFlags must be defined by availableFlags")
		public boolean isSelectionKnown() {
			return availableFlags == null || availableFlags.columns() == null || selectedFlags == null
					|| availableFlags.columns().keySet().containsAll(selectedFlags);
		}
	}
}
