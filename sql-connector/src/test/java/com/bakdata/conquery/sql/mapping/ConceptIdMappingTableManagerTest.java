package com.bakdata.conquery.sql.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

class ConceptIdMappingTableManagerTest {

	@Test
	void shouldRecreateAndPopulateTheMappingTableInTheExistingOrder() {
		List<String> statements = new ArrayList<>();
		MockConnection connection = new MockConnection(context -> {
			statements.addAll(Arrays.asList(context.batchSQL()));
			return new MockResult[] {new MockResult(1, DSL.using(SQLDialect.POSTGRES).newResult())};
		});
		DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
		Field<Integer> resolvedId = DSL.field(DSL.name("resolved_id"), Integer.class);
		Field<String> code = DSL.field(DSL.name("code"), SQLDataType.VARCHAR(12));
		ConceptIdMappingTable mapping = new ConceptIdMappingTable(
				DSL.name("diagnosis_ids"),
				List.of(code),
				List.of(DSL.row(List.of(DSL.val(7), DSL.val("A12"))))
		);

		new ConceptIdMappingTableManager(dsl).recreate(mapping);

		assertEquals(List.of(
				"drop table \"diagnosis_ids\"",
				"create table \"diagnosis_ids\" (\"resolved_id\" int, \"code\" varchar(12))",
				"insert into \"diagnosis_ids\" (\"resolved_id\", \"code\") values (7, 'A12')",
				"create index \"cq_map_44649089_keys\" on \"diagnosis_ids\"(\"code\")",
				"create index \"cq_map_44649089_id\" on \"diagnosis_ids\"(\"resolved_id\")"
		), statements);
		assertEquals(List.of(resolvedId, code), mapping.tableFields());
	}

	@Test
	void shouldKeepSkippingIndexesForClickHouse() {
		List<String> statements = new ArrayList<>();
		MockConnection connection = new MockConnection(context -> {
			statements.addAll(Arrays.asList(context.batchSQL()));
			return new MockResult[] {new MockResult(1, DSL.using(SQLDialect.CLICKHOUSE).newResult())};
		});
		DSLContext dsl = DSL.using(connection, SQLDialect.CLICKHOUSE);
		Field<String> code = DSL.field(DSL.name("code"), SQLDataType.VARCHAR(12));
		ConceptIdMappingTable mapping = new ConceptIdMappingTable(
				DSL.name("diagnosis_ids"),
				List.of(code),
				List.of(DSL.row(List.of(DSL.val(7), DSL.val("A12"))))
		);

		new ConceptIdMappingTableManager(dsl).recreate(mapping);

		assertFalse(statements.stream().anyMatch(statement -> statement.startsWith("create index")));
	}
}
