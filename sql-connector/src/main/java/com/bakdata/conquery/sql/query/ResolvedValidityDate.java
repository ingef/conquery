package com.bakdata.conquery.sql.query;

import java.util.Objects;

/** Physical representation of event validity for a connector. */
public sealed interface ResolvedValidityDate permits ResolvedValidityDate.None, ResolvedValidityDate.Point, ResolvedValidityDate.Range {

	record None() implements ResolvedValidityDate {
	}

	record Point(ResolvedColumn column) implements ResolvedValidityDate {
		public Point {
			requireDateColumn(column, "column");
		}
	}

	record Range(ResolvedColumn start, ResolvedColumn end) implements ResolvedValidityDate {
		public Range {
			requireDateColumn(start, "start");
			requireDateColumn(end, "end");
			if (!start.table().equals(end.table())) {
				throw new IllegalArgumentException("validity-date columns must belong to the same table");
			}
		}
	}

	private static void requireDateColumn(ResolvedColumn column, String name) {
		Objects.requireNonNull(column, name);
		if (column.type() != ColumnType.DATE) {
			throw new IllegalArgumentException(name + " must be a DATE column");
		}
	}
}
