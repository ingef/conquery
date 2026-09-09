package com.bakdata.conquery.sql.model.operation;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Declarative connector and concept-element conditions supported by the SQL connector. */
public final class BuiltInConditions {

	private BuiltInConditions() {
	}

	public record AllOf(
			@NotEmpty List<@NotNull @Valid ResolvedCondition> conditions
	) implements ResolvedCondition {

		public AllOf {
			conditions = ModelNormalization.immutableCopy(conditions);
		}
	}

	public record Not(@NotNull @Valid ResolvedCondition condition) implements ResolvedCondition {
	}

	public record StringValues(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn column,
			@NotEmpty Set<@NotNull String> values
	) implements ResolvedCondition {

		public StringValues {
			values = ModelNormalization.immutableCopy(values);
		}
	}

	public record Presence(@NotNull @Valid ResolvedColumn column, boolean present) implements ResolvedCondition {
	}

	public record Prefixes(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn column,
			@NotEmpty List<@NotBlank String> prefixes
	) implements ResolvedCondition {

		public Prefixes {
			prefixes = ModelNormalization.immutableCopy(prefixes);
		}
	}

	public record PrefixRange(
			@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn column,
			@NotBlank String minimum,
			@NotBlank String maximum
	) implements ResolvedCondition {

		@AssertTrue(message = "prefix bounds must have equal length and be ordered")
		public boolean isOrdered() {
			return minimum == null || maximum == null
					|| minimum.length() == maximum.length() && minimum.compareTo(maximum) <= 0;
		}
	}
}
