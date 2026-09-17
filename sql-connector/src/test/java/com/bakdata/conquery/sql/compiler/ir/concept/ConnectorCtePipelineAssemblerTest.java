package com.bakdata.conquery.sql.compiler.ir.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.junit.jupiter.api.Test;

class ConnectorCtePipelineAssemblerTest {

	@Test
	void shouldAssemblePreprocessingAndPipelineInputs() {
		ConnectorCtePlan plan = plan();
		SqlIdColumns ids = new SqlIdColumns(field(name("events", "person"), String.class));
		ColumnDateRange rawValidityDate = ColumnDateRange.of(
				field(name("events", "valid_from"), Date.class),
				field(name("events", "valid_to"), Date.class)
		);
		FieldWrapper<Integer> directSelect = new FieldWrapper<>(field(name("events", "score"), Integer.class));
		FieldWrapper<Integer> filterSelect = new FieldWrapper<>(field(name("events", "threshold"), Integer.class));
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
				.preprocessingSelect(directSelect)
				.build();
		SqlFilters filters = new SqlFilters(
				ConnectorSqlSelects.builder().preprocessingSelect(filterSelect).build(),
				WhereClauses.builder().build()
		);
		QueryStep stratificationTable = queryStep("stratification");

		ConnectorCtePipelineInput input = ConnectorCtePipelineAssembler.assemble(
				plan,
				ids,
				rawValidityDate,
				List.of(selects),
				List.of(filters),
				Optional.of(stratificationTable)
		);

		assertSame(plan.tables(), input.tables());
		assertEquals(name("events"), input.preprocessing().sourceTable().getQualifiedName());
		assertSame(ids, input.preprocessing().ids());
		assertSame(rawValidityDate, input.preprocessing().rawValidityDate());
		assertEquals("connector_validity_date", input.preprocessing().validityDate().getAlias());
		assertEquals(List.of(selects, filters.getSelects()), input.preprocessing().sqlSelects());
		assertEquals(List.of(filters), input.preprocessing().sqlFilters());
		assertEquals(Optional.of(stratificationTable), input.preprocessing().stratificationTable());
		assertTrue(input.withIntervalPacking());
		assertTrue(input.excludedFromTimeAggregation());
	}

	@Test
	void shouldCollectEventSelectsAndAllAdditionalPredecessors() {
		SqlSelect directEventSelect = new FieldWrapper<>(field(name("direct_event"), Date.class));
		SqlSelect filterEventSelect = new FieldWrapper<>(field(name("filter_event"), Date.class));
		QueryStep directPredecessor = queryStep("direct_predecessor");
		QueryStep filterPredecessor = queryStep("filter_predecessor");
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
				.eventDateSelect(directEventSelect)
				.additionalPredecessor(Optional.of(directPredecessor))
				.build();
		SqlFilters filters = new SqlFilters(
				ConnectorSqlSelects.builder()
						.eventDateSelect(filterEventSelect)
						.additionalPredecessor(Optional.of(filterPredecessor))
						.build(),
				WhereClauses.builder().build()
		);

		ConnectorCtePipelineInput input = ConnectorCtePipelineAssembler.assemble(
				plan(),
				new SqlIdColumns(field(name("person"), String.class)),
				ColumnDateRange.of(field(name("start"), Date.class), field(name("end"), Date.class)),
				List.of(selects),
				List.of(filters),
				Optional.empty()
		);

		assertEquals(List.of(directEventSelect), input.eventDateSelects());
		assertEquals(List.of(directPredecessor, filterPredecessor), input.additionalPredecessors());
	}

	private static ConnectorCtePlan plan() {
		SqlTables tables = new SqlTables(
				"events",
				Map.of(ConceptCteStep.PREPROCESSING, "connector-preprocessing"),
				Map.of()
		);
		return new ConnectorCtePlan("connector", table(name("events")), tables, true, true);
	}

	private static QueryStep queryStep(String cteName) {
		return QueryStep.builder()
				.cteName(cteName)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.build())
				.build();
	}
}
