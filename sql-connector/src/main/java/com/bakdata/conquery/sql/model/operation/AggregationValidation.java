package com.bakdata.conquery.sql.model.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import lombok.experimental.UtilityClass;

@UtilityClass
class AggregationValidation {

	static List<ResolvedColumn> withColumn(ResolvedColumn column, List<ResolvedColumn> other) {
		final List<ResolvedColumn> columns = new ArrayList<>();
		if (column != null) {
			columns.add(column);
		}
		return withColumns(columns, other);
	}

	static List<ResolvedColumn> withColumns(List<ResolvedColumn> columns, List<ResolvedColumn> other) {
		final List<ResolvedColumn> combined = new ArrayList<>(columns);
		if (other != null) {
			combined.addAll(other);
		}
		return combined;
	}

	static boolean sameTable(Collection<ResolvedColumn> columns) {
		return columns.stream()
				.filter(java.util.Objects::nonNull)
				.map(ResolvedColumn::table)
				.distinct()
				.limit(2)
				.count() < 2;
	}
}
