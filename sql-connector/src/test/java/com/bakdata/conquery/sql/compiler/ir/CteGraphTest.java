package com.bakdata.conquery.sql.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CteGraphTest {

	@Test
	void shouldCreateDefaultPredecessorMapIncludingRootSteps() {
		Map<CteStep, CteStep> predecessors = CteStep.getDefaultPredecessorMap(Set.of(Step.ROOT, Step.CHILD));

		assertTrue(predecessors.containsKey(Step.ROOT));
		assertNull(predecessors.get(Step.ROOT));
		assertEquals(Step.ROOT, predecessors.get(Step.CHILD));
	}

	@Test
	void shouldDelegateCteNameGeneration() {
		Map<CteStep, String> names = CteStep.createCteNameMap(
				Set.of(Step.ROOT, Step.CHILD),
				"query",
				(step, label) -> "generated_" + step.cteName(label)
		);

		assertEquals("generated_query-root", names.get(Step.ROOT));
		assertEquals("generated_query-child", names.get(Step.CHILD));
	}

	@Test
	void shouldResolveCteAndPredecessorTables() {
		Map<CteStep, String> names = Map.of(Step.ROOT, "query-root", Step.CHILD, "query-child");
		SqlTables tables = new SqlTables(
				"source_table",
				names,
				CteStep.getDefaultPredecessorMap(Set.of(Step.ROOT, Step.CHILD))
		);

		assertEquals("source_table", tables.getRootTable());
		assertEquals("query-child", tables.cteName(Step.CHILD));
		assertEquals("source_table", tables.getPredecessor(Step.ROOT));
		assertEquals("query-root", tables.getPredecessor(Step.CHILD));
		assertTrue(tables.isRequiredStep(Step.CHILD));
		assertFalse(tables.isRequiredStep(Step.UNUSED));
	}

	private enum Step implements CteStep {
		ROOT("root", null),
		CHILD("child", ROOT),
		UNUSED("unused", null);

		private final String suffix;
		private final CteStep predecessor;

		Step(String suffix, CteStep predecessor) {
			this.suffix = suffix;
			this.predecessor = predecessor;
		}

		@Override
		public String getSuffix() {
			return suffix;
		}

		@Override
		public CteStep getPredecessor() {
			return predecessor;
		}
	}
}
