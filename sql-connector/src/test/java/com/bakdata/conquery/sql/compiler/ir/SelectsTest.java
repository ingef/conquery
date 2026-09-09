package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class SelectsTest {

	@Test
	void shouldBuildImmutableSelectsWithDefaultDates() {
		FieldWrapper<Integer> explicit = new FieldWrapper<>(field(name("value"), Integer.class));
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)))
				.sqlSelect(explicit)
				.build();

		assertTrue(selects.getValidityDate().isEmpty());
		assertTrue(selects.getStratificationDate().isEmpty());
		assertEquals(List.of(explicit), selects.getSqlSelects());
		assertThrows(UnsupportedOperationException.class, () -> selects.getSqlSelects().clear());
	}

	@Test
	void shouldPreserveAdditiveLombokBuilderBehavior() {
		FieldWrapper<Integer> first = new FieldWrapper<>(field(name("first"), Integer.class));
		FieldWrapper<Integer> second = new FieldWrapper<>(field(name("second"), Integer.class));
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)))
				.sqlSelect(first)
				.build();

		Selects extended = selects.toBuilder().sqlSelects(List.of(second)).build();

		assertEquals(List.of(first, second), extended.getSqlSelects());
	}

	@Test
	void shouldQualifyAllCompilerFields() {
		ColumnDateRange validityDate = ColumnDateRange.of(
				field(name("valid_from"), Date.class),
				field(name("valid_to"), Date.class)
		);
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(field(name("person"), String.class)))
				.validityDate(Optional.of(validityDate))
				.sqlSelect(new FieldWrapper<>(field(name("value"), Integer.class)))
				.build();

		Selects qualified = selects.qualify("step");

		assertEquals(
				List.of(name("step", "person"), name("step", "valid_from"), name("step", "valid_to"), name("step", "value")),
				qualified.all().stream().map(Field::getQualifiedName).toList()
		);
		assertFalse(qualified.blockValidityDate().getValidityDate().isPresent());
	}
}
