package com.bakdata.conquery.sql.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSelection;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSource;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingTable;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.node.NegationNode;
import com.bakdata.conquery.sql.model.node.OrNode;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

class DefaultSqlCompilerTest {

	private static final SqlTable ENTITIES = SqlTable.of("entities", "catalog", "entities");
	private static final ResolvedColumn ENTITY_ID = new ResolvedColumn(
			"entity-id", ENTITIES, "person_id", ColumnType.STRING, false
	);

	@Test
	void shouldCompileAllEntitiesWithOrderedColumnMetadata() {
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new AllEntitiesNode(),
				false,
				List.of()
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("\"catalog\".\"entities\""));
		assertEquals(List.of(new CompiledColumn(
				"entity-id", "person_id", ResultType.Primitive.STRING, ColumnRole.ENTITY_ID
		)), compiled.columns());
	}

	@Test
	void shouldCompileAResolvedConceptThroughTheConnectorPipeline() {
		SqlTable events = SqlTable.of("events", "catalog", "events");
		ResolvedConnector connector = new ResolvedConnector(
				"claims",
				events,
				new ResolvedColumn("events.entity-id", events, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.None(),
				List.of(),
				List.of(),
				List.of()
		);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new ConceptNode(
						"diagnosis",
						List.of(connector),
						List.of(new BuiltInSelects.Exists("Diagnosis exists")),
						DateAggregationAction.BLOCK
				),
				false,
				List.of(new ResultColumn("diagnosis.exists", ResultType.Primitive.BOOLEAN))
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("concept_diagnosis_claims-0-preprocessing"));
		assertEquals("diagnosis_exists-1", compiled.columns().get(1).sqlAlias());
		assertEquals("diagnosis.exists", compiled.columns().get(1).outputId());
	}

	@Test
	void shouldApplyConceptSelectionAndResolveConceptIdsInTheConnector() {
		SqlTable events = SqlTable.of("events", "catalog", "events");
		ResolvedColumn conceptColumn = new ResolvedColumn(
				"events.diagnosis", events, "diagnosis", ColumnType.STRING, true
		);
		ConceptIdMappingSource mappingSource = new ConceptIdMappingSource(
				new ConceptIdMappingTable(
						DSL.name("diagnosis_ids"),
						List.of(DSL.field(DSL.name("code"), SQLDataType.VARCHAR)),
						List.of()
				),
				Map.of("code", DSL.field(DSL.name("events", "diagnosis"), String.class))
		);
		ResolvedConnector connector = new ResolvedConnector(
				"claims",
				events,
				new ResolvedColumn("events.entity-id", events, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.None(),
				List.of(),
				List.of(),
				List.of(),
				Optional.of(new ConceptIdMappingSelection(mappingSource, Set.of(4, 7), false, true, 0))
		);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new ConceptNode(
						"diagnosis",
						List.of(connector),
						List.of(new BuiltInSelects.ConceptValues("concept ids", List.of(conceptColumn))),
						DateAggregationAction.BLOCK
				),
				false,
				List.of(new ResultColumn("concept-ids", new ResultType.ListType(ResultType.Primitive.STRING)))
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(
				compiled.sql().contains("\"diagnosis_ids\".\"resolved_id\" in (4, 7)")
						|| compiled.sql().contains("\"diagnosis_ids\".\"resolved_id\" in (7, 4)"),
				compiled.sql()
		);
		assertTrue(compiled.sql().contains("coalesce(\"diagnosis_ids\".\"resolved_id\", 0)"), compiled.sql());
		assertEquals("concept-ids", compiled.columns().get(1).outputId());
	}

	@Test
	void shouldExposeASecondaryIdAsTheFirstResultColumn() {
		SqlTable events = SqlTable.of("events", "catalog", "events");
		ResolvedColumn secondaryId = new ResolvedColumn(
				"events.insurance-id", events, "insurance_id", ColumnType.STRING, false
		);
		ResolvedConnector connector = new ResolvedConnector(
				"claims", events,
				new ResolvedColumn("events.entity-id", events, "person_id", ColumnType.STRING, false),
				Optional.of(secondaryId), new ResolvedValidityDate.None(), List.of(), List.of(), List.of()
		);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new ConceptNode("diagnosis", List.of(connector), List.of(), DateAggregationAction.BLOCK),
				false,
				List.of(new ResultColumn("secondary-id", ResultType.Primitive.STRING))
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertEquals(ColumnRole.ENTITY_ID, compiled.columns().get(0).role());
		assertEquals(new CompiledColumn(
				"secondary-id", "secondary_id", ResultType.Primitive.STRING, ColumnRole.RESULT
		), compiled.columns().get(1));
	}

	@Test
	void shouldKeepNestedNegationStickyLikeTheExistingBackend() {
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new NegationNode(new NegationNode(new AllEntitiesNode())),
				false,
				List.of()
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("all_ids_negated"));
	}

	@Test
	void shouldKeepExternalValuesInDeclaredOrderWhenJoiningLogicalBranches() {
		ExternalNode external = new ExternalNode(
				List.of(new ExternalEntity("person-1", List.of(), Map.of(
						"first value", List.of("a"),
						"second value", List.of("b")
				))),
				List.of("second value", "first value")
		);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new OrNode(List.of(new AllEntitiesNode(), external), DateAggregationAction.BLOCK, false),
				false,
				List.of(
						new ResultColumn("second", ResultType.Primitive.STRING),
						new ResultColumn("first", ResultType.Primitive.STRING)
				)
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("external_extra"));
		assertEquals(List.of("primary_id", "second_value", "first_value"),
				compiled.columns().stream().map(CompiledColumn::sqlAlias).toList());
		assertEquals(List.of("entity-id", "second", "first"),
				compiled.columns().stream().map(CompiledColumn::outputId).toList());
	}

	@Test
	void shouldKeepValidityDateBeforeRepeatedConceptSelectsAcrossConnectors() {
		SqlTable claims = SqlTable.of("claims", "catalog", "claims");
		SqlTable prescriptions = SqlTable.of("prescriptions", "catalog", "prescriptions");
		ResolvedConnector first = new ResolvedConnector(
				"claims", claims,
				new ResolvedColumn("claims.entity-id", claims, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.Point(new ResolvedColumn(
						"claims.date", claims, "event_date", ColumnType.DATE, true
				)),
				List.of(), List.of(), List.of()
		);
		ResolvedConnector second = new ResolvedConnector(
				"prescriptions", prescriptions,
				new ResolvedColumn("prescriptions.entity-id", prescriptions, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.Point(new ResolvedColumn(
						"prescriptions.date", prescriptions, "event_date", ColumnType.DATE, true
				)),
				List.of(), List.of(), List.of()
		);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new ConceptNode(
						"events",
						List.of(first, second),
						List.of(new BuiltInSelects.Exists("found"), new BuiltInSelects.Exists("found")),
						DateAggregationAction.MERGE
				),
				true,
				List.of(
						new ResultColumn("validity", ResultType.Primitive.DATE_RANGE),
						new ResultColumn("first-found", ResultType.Primitive.BOOLEAN),
						new ResultColumn("second-found", ResultType.Primitive.BOOLEAN)
				)
		);

		CompiledQuery compiled = new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("concept_events_claims-0-preprocessing"));
		assertTrue(compiled.sql().contains("concept_events_prescriptions-0-preprocessing"));
		assertEquals(List.of("primary_id", "dates", "found-1", "found-2"),
				compiled.columns().stream().map(CompiledColumn::sqlAlias).toList());
		assertEquals(List.of("entity-id", "validity", "first-found", "second-found"),
				compiled.columns().stream().map(CompiledColumn::outputId).toList());
	}

	@Test
	void shouldCompileConceptEventDateUnionWithBlockedDateAggregation() {
		assertConceptEventDateSelectCompiles(
				new BuiltInSelects.EventDateUnion("event dates"),
				new ResultType.ListType(ResultType.Primitive.DATE_RANGE)
		);
	}

	@Test
	void shouldCompileConceptEventDurationSumWithBlockedDateAggregation() {
		assertConceptEventDateSelectCompiles(
				new BuiltInSelects.EventDurationSum("event duration"),
				ResultType.Primitive.NUMERIC
		);
	}

	private static void assertConceptEventDateSelectCompiles(ResolvedSelect select, ResultType resultType) {
		SqlTable claims = SqlTable.of("claims", "catalog", "claims");
		SqlTable prescriptions = SqlTable.of("prescriptions", "catalog", "prescriptions");
		ResolvedConnector first = connectorWithEventDate("claims", claims);
		ResolvedConnector second = connectorWithEventDate("prescriptions", prescriptions);
		ResolvedQuery query = new ResolvedQuery(
				new EntitySchema(ENTITY_ID),
				new ConceptNode("events", List.of(first, second), List.of(select), DateAggregationAction.BLOCK),
				false,
				List.of(new ResultColumn("event-select", resultType))
		);
		HanaCompilerDialect dialect = new HanaCompilerDialect();
		ResolvedQueryStepCompiler.CompiledQuerySteps compiledSteps = assertDoesNotThrow(
				() -> new ResolvedQueryStepCompiler().compile(query, dialect, new SqlNameGenerator(128), Optional.empty())
		);

		assertTrue(compiledSteps.step().getSelects().getValidityDate().isEmpty());

		CompiledQuery compiled = assertDoesNotThrow(() -> new DefaultSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(query, dialect));

		assertEquals("event-select", compiled.columns().get(1).outputId());
	}

	private static ResolvedConnector connectorWithEventDate(String logicalId, SqlTable table) {
		return new ResolvedConnector(
				logicalId,
				table,
				new ResolvedColumn(logicalId + ".entity-id", table, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.Point(new ResolvedColumn(
						logicalId + ".date", table, "event_date", ColumnType.DATE, true
				)),
				List.of(),
				List.of(),
				List.of()
		);
	}

}
