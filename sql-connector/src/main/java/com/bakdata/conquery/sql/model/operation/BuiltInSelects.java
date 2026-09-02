package com.bakdata.conquery.sql.model.operation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.DateColumns;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Framework-neutral select operations supported by the SQL connector. */
public final class BuiltInSelects {

	private BuiltInSelects() {
	}

	public enum ValueOperation {
		DISTINCT,
		FIRST,
		LAST,
		RANDOM
	}

	public record Aggregation(@NotNull @Valid ResolvedAggregation aggregation) implements ResolvedSelect {
	}

	public record Values(
			@NotNull @Valid ResolvedColumn column,
			@NotNull ValueOperation operation,
			@NotNull Optional<@Valid SubstringRange> substring
	) implements ResolvedSelect {

		@AssertTrue(message = "substring requires a STRING column")
		public boolean isSubstringOnStringColumn() {
			return column == null || substring == null || substring.isEmpty() || column.type() == ColumnType.STRING;
		}
	}

	public record DateUnion(@NotNull @Valid DateColumns dates) implements ResolvedSelect {
	}

	/** Selects the distance to an end date that was frozen while resolving the query. */
	public record DateDistance(
			@NotNull @Valid @AllowedColumnTypes({ColumnType.DATE, ColumnType.DATE_RANGE}) ResolvedColumn column,
			@NotNull ChronoUnit unit,
			@NotNull LocalDate endDate
	) implements ResolvedSelect {
	}

	/** Aggregates the resolved concept-value columns across connector tables. */
	public record ConceptValues(
			@NotEmpty List<@NotNull @Valid ResolvedColumn> columns
	) implements ResolvedSelect {

		public ConceptValues {
			columns = ModelNormalization.immutableCopy(columns);
		}
	}

	public record EventDateUnion() implements ResolvedSelect {
	}

	public record EventDurationSum() implements ResolvedSelect {
	}

	public record Exists() implements ResolvedSelect {
	}
}
