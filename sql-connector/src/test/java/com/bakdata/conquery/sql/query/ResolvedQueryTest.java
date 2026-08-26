package com.bakdata.conquery.sql.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ResolvedQueryTest {

	private static final SqlTable EVENTS = SqlTable.of("dataset.events", "analytics", "events");
	private static final ResolvedColumn ENTITY_ID = new ResolvedColumn(
			"dataset.events.entity_id", EVENTS, "entity_id", ColumnType.STRING, false
	);

	@Test
	void shouldCreateResolvedQueryWithoutFrameworkTypes() {
		ResolvedConnector connector = new ResolvedConnector(
				"dataset.concept.connector",
				EVENTS,
				ENTITY_ID,
				Optional.empty(),
				new ResolvedValidityDate.Point(dateColumn("event_date")),
				List.of(),
				List.of(),
				List.of()
		);
		ConceptNode concept = new ConceptNode(
				"dataset.concept", List.of(connector), List.of(), DateAggregationAction.MERGE
		);

		ResolvedQuery query = new ResolvedQuery(
				new ExecutionTarget("dataset", "warehouse"),
				new EntitySchema(EVENTS, ENTITY_ID),
				concept,
				true,
				List.of(new ResultColumn(
						"Entity", Optional.of("Entity"), Optional.empty(), ResultType.Primitive.STRING, Set.of("ID")
				))
		);

		assertEquals(concept, query.root());
		assertEquals("warehouse", query.target().dataSource());
	}

	@Test
	void shouldDefensivelyCopyOrderedCollections() {
		List<QueryNode> children = new ArrayList<>();
		children.add(conceptNode(EVENTS));
		AndNode andNode = new AndNode(children, DateAggregationAction.INTERSECT, false);

		children.add(conceptNode(EVENTS));

		assertEquals(1, andNode.children().size());
		assertThrows(UnsupportedOperationException.class, () -> andNode.children().clear());
	}

	@Test
	void shouldRejectColumnsFromAnotherConnectorTable() {
		SqlTable otherTable = SqlTable.of("dataset.other", "analytics", "other");
		ResolvedColumn otherId = new ResolvedColumn(
				"dataset.other.entity_id", otherTable, "entity_id", ColumnType.STRING, false
		);

		assertThrows(IllegalArgumentException.class, () -> new ResolvedConnector(
				"dataset.concept.connector",
				EVENTS,
				otherId,
				Optional.empty(),
				new ResolvedValidityDate.None(),
				List.of(),
				List.of(),
				List.of()
		));
	}

	@Test
	void shouldRejectNonDateValidityColumns() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new ResolvedValidityDate.Point(ENTITY_ID)
		);
	}

	@Test
	void shouldRejectInvertedDateRestriction() {
		assertThrows(IllegalArgumentException.class, () -> DateRange.closed(
				LocalDate.of(2025, 2, 1),
				LocalDate.of(2025, 1, 1)
		));
	}

	private static ConceptNode conceptNode(SqlTable table) {
		ResolvedColumn id = new ResolvedColumn("concept.id", table, "entity_id", ColumnType.STRING, false);
		ResolvedConnector connector = new ResolvedConnector(
				"concept.connector", table, id, Optional.empty(), new ResolvedValidityDate.None(),
				List.of(), List.of(), List.of()
		);
		return new ConceptNode("concept", List.of(connector), List.of(), DateAggregationAction.BLOCK);
	}

	private static ResolvedColumn dateColumn(String name) {
		return new ResolvedColumn("dataset.events." + name, EVENTS, name, ColumnType.DATE, true);
	}
}
