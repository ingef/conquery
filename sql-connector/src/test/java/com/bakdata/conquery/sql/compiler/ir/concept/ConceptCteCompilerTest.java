package com.bakdata.conquery.sql.compiler.ir.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ConceptCteCompilerTest {

	@Test
	void shouldCompileIntervalSelectBranchFromResolvedConceptInput() {
		QueryStep predecessor = queryStep("connector");
		FieldWrapper<Integer> eventDuration = new FieldWrapper<>(field(name("event_duration"), Integer.class));
		ConceptSqlSelects conceptSelects = ConceptSqlSelects.builder()
				.eventDateSelect(eventDuration)
				.build();
		SqlTables tables = new SqlTables(
				"connector",
				Map.of(
						ConceptCteStep.INTERVAL_PACKING_SELECTS, "interval_packing",
						ConceptCteStep.UNIVERSAL_SELECTS, "concept"
				),
				Map.of()
		);

		QueryStep result = ConceptCteCompiler.compileConcept(new ConceptCteInput(
				predecessor,
				List.of(conceptSelects),
				tables,
				false
		));

		assertEquals(List.of("connector", "interval_packing"), result.getPredecessors().stream().map(QueryStep::getCteName).toList());
		QueryStep intervalPacking = result.getPredecessors().getLast();
		assertEquals(List.of(eventDuration), intervalPacking.getSelects().getSqlSelects());
	}

	@Test
	void shouldCompileUniversalConceptStep() {
		FieldWrapper<Integer> connectorMetric = new FieldWrapper<>(field(name("connector_metric"), Integer.class));
		QueryStep predecessor = queryStep("connector", connectorMetric);
		QueryStep additional = queryStep("additional");
		QueryStep intervalPacking = queryStep("interval_packing");
		FieldWrapper<Integer> conceptMetric = new FieldWrapper<>(field(name("concept_metric"), Integer.class));
		ExistsSqlSelect universal = ExistsSqlSelect.withAlias("exists");
		ConceptSqlSelects conceptSelects = ConceptSqlSelects.builder()
				.additionalPredecessor(Optional.of(additional))
				.finalSelect(conceptMetric)
				.finalSelect(universal)
				.build();
		SqlTables tables = new SqlTables(
				"connector",
				Map.of(ConceptCteStep.UNIVERSAL_SELECTS, "concept"),
				Map.of()
		);

		QueryStep result = ConceptCteCompiler.compileUniversalSelects(
				predecessor,
				List.of(conceptSelects),
				Optional.of(intervalPacking),
				tables,
				true
		);

		assertEquals("concept", result.getCteName());
		assertEquals(List.of(predecessor, additional, intervalPacking), result.getPredecessors());
		assertTrue(result.isNegate());
		List<SqlSelect> sqlSelects = result.getSelects().getSqlSelects();
		assertSame(conceptMetric, sqlSelects.getFirst());
		assertSame(universal, sqlSelects.get(1));
		assertEquals(name("connector", "connector_metric"), sqlSelects.getLast().toFields().getFirst().getQualifiedName());
		assertEquals(
				List.of(
						name("connector", "person"),
						name("connector", "validity_start"),
						name("connector", "validity_end"),
						name("concept_metric"),
						name("connector", "connector_metric")
				),
				result.getGroupBy().stream().map(Field::getQualifiedName).toList()
		);
		String renderedFrom = DSL.using(SQLDialect.POSTGRES)
				.select()
				.from(result.getFromTables().getFirst())
				.getSQL()
				.toLowerCase(Locale.ROOT);
		assertTrue(renderedFrom.contains("from \"connector\" join \"additional\""));
		assertTrue(renderedFrom.contains("join \"interval_packing\""));
	}

	private static QueryStep queryStep(String cteName, SqlSelect... sqlSelects) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.validityDate(Optional.of(ColumnDateRange.of(
								field(name("validity_start"), Date.class),
								field(name("validity_end"), Date.class)
						)))
						.sqlSelects(List.of(sqlSelects))
						.build())
				.build();
	}
}
