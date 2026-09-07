package com.bakdata.conquery.sql.compiler.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.junit.jupiter.api.Test;

class SqlNameGeneratorTest {

	private static final SqlTable EVENTS = SqlTable.of("events", "analytics", "events");
	private static final ResolvedColumn PRIMARY_ID = new ResolvedColumn(
			"events.id", EVENTS, "id", ColumnType.STRING, false
	);
	private static final ResolvedConnector CONNECTOR = new ResolvedConnector(
			"Events",
			EVENTS,
			PRIMARY_ID,
			Optional.empty(),
			new ResolvedValidityDate.None(),
			List.of(),
			List.of(),
			List.of()
	);
	private static final ConceptNode CONCEPT = new ConceptNode(
			"My Concept", List.of(CONNECTOR), List.of(), DateAggregationAction.BLOCK
	);

	@Test
	void shouldNormalizeAndDisambiguateOperationNames() {
		SqlNameGenerator generator = new SqlNameGenerator(64);

		assertEquals("total_count-1", generator.selectName(new TestSelect("Total Count")));
		assertEquals("total_count-2", generator.filterName(new TestFilter("Total Count")));
	}

	@Test
	void shouldKeepConceptAndConnectorNamesInTheSameSequence() {
		SqlNameGenerator generator = new SqlNameGenerator(64);

		assertEquals("concept_my_concept-1", generator.conceptName(CONCEPT));
		assertEquals("concept_my_concept_events-1", generator.conceptConnectorName(CONCEPT, CONNECTOR));
	}

	@Test
	void shouldCountJoinedNodeTypesIndependently() {
		SqlNameGenerator generator = new SqlNameGenerator(64);

		assertEquals("AND-1", generator.joinedNodeName(JoinMode.INNER));
		assertEquals("OR-1", generator.joinedNodeName(JoinMode.FULL_OUTER));
		assertEquals("AND-2", generator.joinedNodeName(JoinMode.INNER));
		assertThrows(UnsupportedOperationException.class, () -> generator.joinedNodeName(JoinMode.LEFT));
	}

	@Test
	void shouldLimitGeneratedNamesFromTheRight() {
		SqlNameGenerator generator = new SqlNameGenerator(8);

		assertEquals("_label-1", generator.selectName(new TestSelect("Long Label")));
		assertEquals("fix-step", generator.cteStepName(Step.VALUE, "long-prefix"));
	}

	private record TestSelect(String name) implements ResolvedSelect {
	}

	private record TestFilter(String name) implements ResolvedFilter {
	}

	private enum Step implements CteStep {
		VALUE;

		@Override
		public String getSuffix() {
			return "step";
		}
	}
}
