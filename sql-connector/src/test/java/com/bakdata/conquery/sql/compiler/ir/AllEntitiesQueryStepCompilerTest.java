package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.Field;
import org.junit.jupiter.api.Test;

class AllEntitiesQueryStepCompilerTest {

	private static final SqlTable ENTITIES = SqlTable.of("entities", "catalog", "entities");
	private static final EntitySchema ENTITY_SCHEMA = new EntitySchema(
			new ResolvedColumn("entity-id", ENTITIES, "person_id", ColumnType.STRING, false)
	);

	@Test
	void shouldCompileResolvedEntitySchema() {
		QueryStep step = AllEntitiesQueryStepCompiler.compile(
				new AllEntitiesNode(),
				ENTITY_SCHEMA,
				new TestDialect()
		);

		assertEquals("all_ids", step.getCteName());
		assertEquals(
				name("catalog", "entities", "person_id"),
				step.getSelects().getIds().getPrimaryColumn().getQualifiedName()
		);
		assertEquals(name("catalog", "entities"), step.getFromTables().getFirst().asTable().getQualifiedName());
		ColumnDateRange validityDate = step.getSelects().getValidityDate().orElseThrow();
		assertEquals("all_ids_validity_date_start", validityDate.getStart().getName());
		assertEquals("all_ids_validity_date_end", validityDate.getEnd().getName());
	}

	private static final class TestDialect implements CompilerDialect {

		@Override
		public Field<Date> minimumDate() {
			return field(name("minimum_date"), Date.class);
		}

		@Override
		public Field<Date> maximumDate() {
			return field(name("maximum_date"), Date.class);
		}

		@Override
		public <T> Field<T> anyValue(Field<T> value) {
			return value;
		}

		@Override
		public Field<?> renderDateRange(Field<Date> start, Field<Date> end) {
			return field(name("rendered_range"), Object.class);
		}

		@Override
		public Field<?> aggregateDateRanges(Field<Date> start, Field<Date> end) {
			return field(name("aggregated_ranges"), Object.class);
		}

		@Override
		public int getNameMaxLength() {
			return 128;
		}
	}
}
