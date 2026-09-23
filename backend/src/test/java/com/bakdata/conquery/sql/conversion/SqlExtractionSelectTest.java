package com.bakdata.conquery.sql.conversion;

import static com.bakdata.conquery.sql.conversion.SqlExtractionAggregationTest.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.model.select.FirstValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.LastValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.RandomValueSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.DistinctSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDistinctSelectConverter;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import org.junit.jupiter.api.Test;

class SqlExtractionSelectTest {

	@Test
	void shouldDelegateExistsForConnectorAndConceptProjections() {
		var select = new ExistsSelect();
		select.setName("exists");
		var connectorResult = new ExistsSelectConverter().connectorSelect(select, context());
		var base = context();
		var conceptContext = SelectContext.create(base.getIds(), java.util.Optional.empty(),
				new ConceptSqlTables(base.getTables(), java.util.List.of()), base.getConversionContext());
		var conceptResult = new ExistsSelectConverter().conceptSelect(select, conceptContext);

		assertEquals("1 as \"exists-1\"", render(connectorResult.getFinalSelects().getFirst().toFields().getFirst()));
		assertEquals("1 as \"exists-1\"", render(conceptResult.getFinalSelects().getFirst().toFields().getFirst()));
		assertEquals(1, connectorResult.getFinalSelects().size());
		assertEquals(1, conceptResult.getFinalSelects().size());
	}

	@Test
	void shouldOrderLastByBothValidityBoundsWithNullsLast() {
		var base = context();
		var dates = com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange.of(
				org.jooq.impl.DSL.field(org.jooq.impl.DSL.name("start"), java.sql.Date.class),
				org.jooq.impl.DSL.field(org.jooq.impl.DSL.name("end"), java.sql.Date.class)).as("valid");
		var context = com.bakdata.conquery.sql.conversion.model.select.SelectContext.create(
				base.getIds(), java.util.Optional.of(dates), base.getTables(), base.getConversionContext());
		var select = new LastValueSelect(column("value", MajorTypeId.STRING), null, null);
		select.setName("value");
		var result = new LastValueSelectConverter().connectorSelect(select, context);
		var rowNumber = result.getAdditionalPredecessor().orElseThrow().getPredecessors().getFirst();
		String expression = render(rowNumber.getSelects().getSqlSelects().getLast().toFields().getFirst());
		assertTrue(expression.contains("\"preprocessing\".\"valid_start\""));
		assertTrue(expression.contains("\"preprocessing\".\"valid_end\""));
		assertEquals(2, expression.split("desc nulls last", -1).length - 1);
	}

	@Test
	void shouldPreserveDistinctSeparatePredecessor() {
		var select = new DistinctSelect(column("value", MajorTypeId.STRING), null, new Range.IntegerRange(2, 5));
		select.setName("value");
		var result = new DistinctSelectConverter().connectorSelect(select, context());
		var aggregated = result.getAdditionalPredecessor().orElseThrow();
		assertEquals("value-1-aggregated", aggregated.getCteName());
		assertTrue(aggregated.getPredecessors().getFirst().isSelectDistinct());
		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals("substring(\"events\".\"value\", 3, 3) as \"value-1\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
		assertTrue(render(aggregated.getSelects().getSqlSelects().getFirst().toFields().getFirst()).contains("string_agg("));
	}

	@Test
	void shouldPreserveClickhouseDistinctFilteringAndGrouping() {
		var select = new DistinctSelect(column("value", MajorTypeId.STRING), null, null);
		select.setName("value");
		var result = new ClickhouseDistinctSelectConverter().connectorSelect(select, context(new ClickhouseDialectBundle()));
		assertEquals("arrayfilter(x -> x <> '' and x is not null, groupuniqarray(\"preprocessing\".\"value-1\")) as \"value-1\"",
				render(result.getAggregationSelects().getFirst().toFields().getFirst()));
		assertEquals("\"aggregation\".\"value-1\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
		assertTrue(result.getAdditionalPredecessor().isEmpty());
	}

	@Test
	void shouldPreserveFirstSubstringAndFallbackOrdering() {
		var select = new FirstValueSelect(column("value", MajorTypeId.STRING), null, new Range.IntegerRange(2, 5));
		select.setName("value");
		var result = new FirstValueSelectConverter().connectorSelect(select, context());
		assertEquals("substring(\"events\".\"value\", 3, 3) as \"value-1\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
		assertFallbackOrdering(result);
	}

	@Test
	void shouldPreserveLastFallbackOrderingWithoutValidityDate() {
		var select = new LastValueSelect(column("value", MajorTypeId.STRING), null, null);
		select.setName("value");
		var result = new LastValueSelectConverter().connectorSelect(select, context());
		assertFallbackOrdering(result);
	}

	@Test
	void shouldPreserveRandomIgnoringSubstring() {
		var select = new RandomValueSelect(column("value", MajorTypeId.STRING), null, new Range.IntegerRange(2, 5));
		select.setName("value");
		var result = new RandomValueSelectConverter().connectorSelect(select, context());
		assertEquals("\"events\".\"value\"", render(result.getPreprocessingSelects().getFirst().toFields().getFirst()));
		assertEquals("first_value(\"preprocessing\".\"value\" order by rand()) as \"value-1\"", render(result.getAggregationSelects().getFirst().toFields().getFirst()));
		assertEquals("\"aggregation\".\"value-1\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
		assertTrue(result.getAdditionalPredecessor().isEmpty());
	}

	private static void assertFallbackOrdering(ConnectorSqlSelects result) {
		var rowFilter = result.getAdditionalPredecessor().orElseThrow();
		var rowNumber = rowFilter.getPredecessors().getFirst();
		assertEquals("value-1-value_select_first_row_step", rowFilter.getCteName());
		assertEquals("row_number() over (partition by \"preprocessing\".\"id\" order by \"preprocessing\".\"id\") as \"row-number\"",
				render(rowNumber.getSelects().getSqlSelects().getLast().toFields().getFirst()));
		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals(1, result.getFinalSelects().size());
	}
}
