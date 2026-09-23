package com.bakdata.conquery.sql.conversion;

import static com.bakdata.conquery.sql.conversion.SqlExtractionAggregationTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.select.ConceptColumnSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import org.junit.jupiter.api.Test;

class SqlExtractionConceptSelectTest {
	@Test
	void shouldSelectResolvedConceptIdsFromPreparedMapping() {
		var base = context();
		var connector = new ConceptTreeConnector();
		connector.setColumn(column("value", MajorTypeId.STRING));
		var concept = concept(connector);
		var tables = new ConnectorSqlTables(connector, base.getTables().getPlan());
		var context = SelectContext.create(base.getIds(), Optional.empty(), tables, base.getConversionContext());
		var select = new ConceptColumnSelect();
		select.setName("concept");
		select.setAsIds(true);
		select.setHolder(concept);

		var result = new ConceptColumnSelectConverter().connectorSelect(select, context);

		assertEquals("coalesce(\"test.concept_ids\".\"resolved_id\", 0) as \"value\"",
				render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldSelfUnionOneConnectorDistinctlyBeforeStringAggregation() {
		var base = context();
		var connector = new ConceptTreeConnector();
		connector.setColumn(column("value", MajorTypeId.STRING));
		var concept = concept(connector);
		var tables = new ConceptSqlTables(base.getTables(), List.of(new ConnectorSqlTables(connector, base.getTables().getPlan())));
		var context = SelectContext.create(base.getIds(), Optional.empty(), tables, base.getConversionContext());
		var select = new ConceptColumnSelect();
		select.setName("concept");
		select.setHolder(concept);
		var result = new ConceptColumnSelectConverter().conceptSelect(select, context);
		var aggregate = result.getAdditionalPredecessor().orElseThrow();
		var union = aggregate.getPredecessors().getFirst();
		assertEquals(1, union.getUnion().size());
		assertFalse(union.isUnionAll());
		assertTrue(render(union.getSelects().getSqlSelects().getFirst().toFields().getFirst()).contains("\"preprocessing\".\"value\""));
		assertEquals("concept-1-concept_column_aggregated", aggregate.getCteName());
	}

	@Test
	void shouldUseOnlyConnectorsFromConvertedCqTables() {
		var base = context();
		var convertedConnector = new ConceptTreeConnector();
		convertedConnector.setColumn(column("value", MajorTypeId.STRING));
		var unconvertedConnector = new ConceptTreeConnector();
		unconvertedConnector.setColumn(column("other_value", MajorTypeId.STRING));
		var concept = concept(convertedConnector, unconvertedConnector);
		var tables = new ConceptSqlTables(base.getTables(), List.of(new ConnectorSqlTables(convertedConnector, base.getTables().getPlan())));
		var context = SelectContext.create(base.getIds(), Optional.empty(), tables, base.getConversionContext());
		var select = new ConceptColumnSelect();
		select.setName("concept");
		select.setHolder(concept);

		var result = new ConceptColumnSelectConverter().conceptSelect(select, context);
		var union = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();

		assertFalse(render(union.getUnion().getFirst().getSelects().getSqlSelects().getFirst().toFields().getFirst())
				.contains("other_value"));
	}

	private static TreeConcept concept(ConceptTreeConnector... connectors) {
		TreeConcept concept = new TreeConcept() {
			@Override
			public ConceptId getId() {
				return new ConceptId(new DatasetId("test"), "concept");
			}
		};
		concept.setConnectors(List.of(connectors));
		for (ConceptTreeConnector connector : connectors) {
			connector.setConcept(concept);
		}
		return concept;
	}
}
