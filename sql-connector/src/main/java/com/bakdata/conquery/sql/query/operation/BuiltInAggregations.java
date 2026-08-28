package com.bakdata.conquery.sql.query.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.query.internal.ModelNormalization;
import com.bakdata.conquery.sql.query.schema.DateColumns;
import com.bakdata.conquery.sql.query.schema.ResolvedColumn;
import com.bakdata.conquery.sql.query.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Framework-neutral aggregation operations supported by the SQL connector. */
public final class BuiltInAggregations {

	private BuiltInAggregations() {
	}

	public record Count(
			@NotNull @Valid ResolvedColumn column,
			@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
	) implements ResolvedAggregation {

		public Count {
			distinctBy = ModelNormalization.immutableCopy(distinctBy);
		}

		@AssertTrue(message = "count columns must belong to the same table")
		public boolean isOnSameTable() {
			return sameTable(withColumn(column, distinctBy));
		}
	}

	public record CountQuarters(@NotNull @Valid DateColumns dates) implements ResolvedAggregation {
	}

	public record DurationSum(
			@NotNull @Valid DateColumns dates,
			@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
	) implements ResolvedAggregation {

		public DurationSum {
			distinctBy = ModelNormalization.immutableCopy(distinctBy);
		}

		@AssertTrue(message = "duration-sum columns must belong to the same table")
		public boolean isOnSameTable() {
			return dates == null || sameTable(withColumns(dates.columns(), distinctBy));
		}
	}

	public record Sum(
			@NotNull @Valid @AllowedColumnTypes({ColumnType.INTEGER, ColumnType.REAL, ColumnType.DECIMAL, ColumnType.MONEY})
			ResolvedColumn column,
			@NotNull Optional<@Valid @AllowedColumnTypes({
					ColumnType.INTEGER, ColumnType.REAL, ColumnType.DECIMAL, ColumnType.MONEY
			}) ResolvedColumn> subtractColumn,
			@NotNull List<@NotNull @Valid ResolvedColumn> distinctBy
	) implements ResolvedAggregation {

		public Sum {
			distinctBy = ModelNormalization.immutableCopy(distinctBy);
		}

		@AssertTrue(message = "column and subtractColumn must have the same type")
		public boolean isMatchingColumnType() {
			return column == null || subtractColumn == null || subtractColumn.isEmpty()
					|| column.type() == subtractColumn.get().type();
		}

		@AssertTrue(message = "sum columns must belong to the same table")
		public boolean isOnSameTable() {
			if (subtractColumn == null) {
				return true;
			}
			List<ResolvedColumn> columns = withColumn(column, distinctBy);
			subtractColumn.ifPresent(columns::add);
			return sameTable(columns);
		}
	}

	public record Flags(
			@NotEmpty Map<@NotBlank String, @NotNull @Valid @AllowedColumnTypes(ColumnType.BOOLEAN) ResolvedColumn> columns
	) implements ResolvedAggregation {

		public Flags {
			columns = ModelNormalization.immutableCopy(columns);
		}

		@AssertTrue(message = "flag columns must be unique")
		public boolean isUnique() {
			return columns == null || columns.values().stream().distinct().count() == columns.size();
		}

		@AssertTrue(message = "flag columns must belong to the same table")
		public boolean isOnSameTable() {
			return columns == null || sameTable(columns.values());
		}
	}

	private static List<ResolvedColumn> withColumn(ResolvedColumn column, List<ResolvedColumn> other) {
		List<ResolvedColumn> columns = new ArrayList<>();
		if (column != null) {
			columns.add(column);
		}
		return withColumns(columns, other);
	}

	private static List<ResolvedColumn> withColumns(List<ResolvedColumn> columns, List<ResolvedColumn> other) {
		List<ResolvedColumn> combined = new ArrayList<>(columns);
		if (other != null) {
			combined.addAll(other);
		}
		return combined;
	}

	private static boolean sameTable(Collection<ResolvedColumn> columns) {
		return columns.stream()
				.filter(java.util.Objects::nonNull)
				.map(ResolvedColumn::table)
				.distinct()
				.limit(2)
				.count() < 2;
	}
}
