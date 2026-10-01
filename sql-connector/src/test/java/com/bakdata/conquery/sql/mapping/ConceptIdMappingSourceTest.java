package com.bakdata.conquery.sql.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

class ConceptIdMappingSourceTest {

	private static final Table<org.jooq.Record> EVENTS = DSL.table(DSL.name("events"));
	private static final Field<String> MAPPING_CODE = DSL.field(DSL.name("code"), SQLDataType.VARCHAR(12));
	private static final Field<String> EVENT_CODE = DSL.field(DSL.name("events", "diagnosis"), String.class);
	private static final ConceptIdMappingTable MAPPING = new ConceptIdMappingTable(
			DSL.name("diagnosis_ids"), List.of(MAPPING_CODE), List.of()
	);

	@Test
	void shouldFilterSelectedConceptIdsWithAnInnerJoin() {
		ConceptIdMappingSource source = new ConceptIdMappingSource(MAPPING, Map.of("code", EVENT_CODE));

		Table<?> selected = source.selectedConcepts(
				EVENTS, new LinkedHashSet<>(List.of(4, 7)), false, false, new HanaCompilerDialect()
		);

		assertEquals(
				"\"events\" join \"diagnosis_ids\" on (\"diagnosis_ids\".\"code\" = \"events\".\"diagnosis\" and \"diagnosis_ids\".\"resolved_id\" in (4, 7))",
				render(selected)
		);
	}

	@Test
	void shouldKeepTheConnectorTableWhenTheRootIsSelectedWithoutIdResolution() {
		ConceptIdMappingSource source = new ConceptIdMappingSource(MAPPING, Map.of("code", EVENT_CODE));

		Table<?> selected = source.selectedConcepts(
				EVENTS, Set.of(0, 4, 7), true, false, new HanaCompilerDialect()
		);

		assertEquals("\"events\"", render(selected));
	}

	@Test
	void shouldLeftJoinAndFallBackToTheRootIdForConceptIdOutput() {
		ConceptIdMappingSource source = new ConceptIdMappingSource(MAPPING, Map.of("code", EVENT_CODE));

		TableLike<?> resolved = source.resolvedConceptIds(EVENTS, new HanaCompilerDialect());

		assertEquals(
				"\"events\" left outer join \"diagnosis_ids\" on \"diagnosis_ids\".\"code\" = \"events\".\"diagnosis\"",
				render(resolved)
		);
		assertEquals("coalesce(\"diagnosis_ids\".\"resolved_id\", 3)", render(source.resolvedIdOrRoot(3)));
	}

	@Test
	void shouldUseTheHanaJoinConditionWhenTheMappingHasNoKeys() {
		ConceptIdMappingTable rootOnly = new ConceptIdMappingTable(DSL.name("root_ids"), List.of(), List.of());
		ConceptIdMappingSource source = new ConceptIdMappingSource(rootOnly, Map.of());

		TableLike<?> resolved = source.resolvedConceptIds(EVENTS, new HanaCompilerDialect());

		assertEquals("\"events\" left outer join \"root_ids\" on true = true", render(resolved));
	}

	private static String render(org.jooq.QueryPart part) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(part);
	}
}
