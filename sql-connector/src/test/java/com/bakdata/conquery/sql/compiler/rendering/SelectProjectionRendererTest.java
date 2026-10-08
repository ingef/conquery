package com.bakdata.conquery.sql.compiler.rendering;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.ProjectionMode;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class SelectProjectionRendererTest {

	@Test
	void shouldAggregateValidityDatesForGroupedResults() {
		RecordingDialect dialect = new RecordingDialect();

		List<Field<?>> fields = SelectProjectionRenderer.renderFinal(selectsWithDates(), dialect, ProjectionMode.AGGREGATED);

		assertEquals(
				List.of(
						"person",
						SharedAliases.STRATIFICATION_BOUNDS.getAlias(),
						SharedAliases.DATES_COLUMN.getAlias(),
						"value"
				),
				fields.stream().map(Field::getName).toList()
		);
		assertEquals(1, dialect.aggregatedRanges);
		assertEquals(1, dialect.renderedRanges);
	}

	@Test
	void shouldRenderValidityDatesWithoutAggregation() {
		RecordingDialect dialect = new RecordingDialect();

		SelectProjectionRenderer.renderFinal(selectsWithDates(), dialect, ProjectionMode.INDIVIDUAL);

		assertEquals(0, dialect.aggregatedRanges);
		assertEquals(2, dialect.renderedRanges);
	}

	@Test
	void shouldRejectIntermediateProjectionAsFinalOutput() {
		RecordingDialect dialect = new RecordingDialect();

		assertThrows(
				IllegalArgumentException.class,
				() -> SelectProjectionRenderer.renderFinal(
						Selects.builder()
								.ids(new SqlIdColumns(field(name("person"), String.class)))
								.build(),
						dialect,
						ProjectionMode.INTERMEDIATE
				)
		);
	}

	private static Selects selectsWithDates() {
		ColumnDateRange validityDate = ColumnDateRange.of(
				field(name("valid_from"), Date.class),
				field(name("valid_to"), Date.class)
		);
		ColumnDateRange stratificationDate = ColumnDateRange.of(
				field(name("window_from"), Date.class),
				field(name("window_to"), Date.class)
		);
		return Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)))
				.validityDate(Optional.of(validityDate))
				.stratificationDate(Optional.of(stratificationDate))
				.sqlSelect(new FieldWrapper<>(field(name("value"), Integer.class)))
				.build();
	}

	private static final class RecordingDialect implements CompilerDialect {

		private int renderedRanges;
		private int aggregatedRanges;

		@Override
		public Field<Date> minimumDate() {
			return field(name("minimum_date"), Date.class);
		}

		@Override
		public Field<Date> maximumDate() {
			return field(name("maximum_date"), Date.class);
		}

		@Override
		public <T> Field<T> anyValue(Field<T> field) {
			return field;
		}

		@Override
		public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
			this.renderedRanges++;
			return field(name("rendered_range"), Object.class);
		}

		@Override
		public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
			this.aggregatedRanges++;
			return field(name("aggregated_ranges"), Object.class);
		}

		@Override
		public int getNameMaxLength() {
			return 128;
		}
	}
}
