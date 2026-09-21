package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePlan;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.aggregator.CountQuartersSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.aggregator.DurationSumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

/** Exercises application adapters without a repository or database connection. */
class SqlExtractionAggregationTest {

	@Test
	void shouldKeepDistinctSumSubtractionAndPredecessorBehavior() {
		SumSelect select = new SumSelect(column("amount", MajorTypeId.INTEGER), column("discount", MajorTypeId.INTEGER));
		select.setName("total");
		select.setDistinctByColumn(List.of(column("key", MajorTypeId.STRING)));
		var result = new SumSqlAggregator<Range.LongRange>().connectorSelect(select, context());
		var predecessor = result.getAdditionalPredecessor().orElseThrow();

		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals("total-1-row_number_filtered", predecessor.getCteName());
		assertEquals("sum(coalesce(\"total-1-row_number_assigned\".\"amount\", 0)) as \"total-1\"",
				render(predecessor.getSelects().getSqlSelects().getFirst().toFields().getFirst()));
		assertEquals(List.of("\"events\".\"amount\"", "\"events\".\"key\""), result.getPreprocessingSelects().stream()
				.map(value -> render(value.toFields().getFirst())).toList());
		assertEquals(Integer.class, result.getPreprocessingSelects().getFirst().toFields().getFirst().getType());
		assertEquals("\"aggregation\".\"total-1\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldKeepNullAwareOrdinarySumSubtraction() {
		SumSelect select = new SumSelect(column("amount", MajorTypeId.MONEY), column("discount", MajorTypeId.MONEY));
		select.setName("total");
		var result = new SumSqlAggregator<Range.LongRange>().connectorSelect(select, context());
		String zero = "coalesce((\"preprocessing\".\"amount\" * 0), (\"preprocessing\".\"discount\" * 0))";
		assertEquals("sum((coalesce(\"preprocessing\".\"amount\", " + zero + ") - coalesce(\"preprocessing\".\"discount\", " + zero + "))) as \"total-1\"",
				render(result.getAggregationSelects().getFirst().toFields().getFirst()));
		assertTrue(result.getAdditionalPredecessor().isEmpty());
	}

	@Test
	void shouldCountDistinctColumnRatherThanConfiguredDistinctKeys() {
		CountSelect select = new CountSelect();
		select.setName("count");
		select.setColumn(column("value", MajorTypeId.STRING));
		select.setDistinct(true);
		select.setDistinctByColumn(List.of(column("key", MajorTypeId.STRING)));
		var result = new CountSqlAggregator().connectorSelect(select, context());
		assertEquals("nullif(count(distinct \"preprocessing\".\"value\"), 0) as \"count-1\"",
				render(result.getAggregationSelects().getFirst().toFields().getFirst()));
	}

	private static SelectContext<ConnectorSqlTables> context() {
		return context(new HanaDialectBundle());
	}

	@Test
	void shouldSumQuartersPerEventForDatePairs() {
		CountQuartersSelect select = new CountQuartersSelect();
		select.setName("quarters");
		select.setStartColumn(column("start", MajorTypeId.DATE));
		select.setEndColumn(column("end", MajorTypeId.DATE));
		var result = new CountQuartersSqlAggregator().connectorSelect(select, context());
		assertEquals("nullif(sum(\"preprocessing\".\"quarters-1\"), 0) as \"quarters-1\"",
				render(result.getAggregationSelects().getFirst().toFields().getFirst()));
		assertTrue(result.getAdditionalPredecessor().isEmpty());
	}

	@Test
	void shouldPackDurationIntervalsWithoutApplyingDistinctKeys() {
		DurationSumSelect select = new DurationSumSelect();
		select.setName("duration");
		select.setStartColumn(column("start", MajorTypeId.DATE));
		select.setEndColumn(column("end", MajorTypeId.DATE));
		select.setDistinctBy(List.of(column("key", MajorTypeId.STRING)));
		var result = new DurationSumSqlAggregator().connectorSelect(select, context(new ClickhouseDialectBundle()));
		assertEquals("duration-1-interval_packing_selects", result.getAdditionalPredecessor().orElseThrow().getCteName());
		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals(2, result.getPreprocessingSelects().size());
		assertTrue(render(result.getPreprocessingSelects().getFirst().toFields().getFirst()).contains("::nullable(date32)"));
		assertEquals("\"aggregation\".\"duration-1\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
	}

	private static SelectContext<ConnectorSqlTables> context(LegacyCompilerDialect dialect) {
		var conversion = ConversionContext.builder()
				.compilerDialect(dialect).stratificationFunctions(dialect.getStratificationFunctions())
				.nameGenerator(new SqlNameGenerator(127)).build();
		var tables = new SqlTables("events",
				Map.of(ConceptCteStep.PREPROCESSING, "preprocessing", ConceptCteStep.AGGREGATION_SELECT, "aggregation"),
				Map.of(ConceptCteStep.AGGREGATION_SELECT, ConceptCteStep.PREPROCESSING,
						ConceptCteStep.AGGREGATION_FILTER, ConceptCteStep.AGGREGATION_SELECT));
		var plan = new ConnectorCtePlan("events", DSL.table(DSL.name("events")), tables, false, false);
		return SelectContext.create(new SqlIdColumns(DSL.field(DSL.name("events", "id"), String.class)),
				Optional.empty(), new ConnectorSqlTables(null, plan), conversion);
	}

	private static ColumnId column(String name, MajorTypeId type) {
		Table table = new Table();
		table.setName("events");
		table.setDataset(new DatasetId("test"));
		Column column = new Column();
		column.setName(name);
		column.setType(type);
		column.setTable(table);
		// Replace only repository access; conversion and SQL construction remain real.
		return new ColumnId(table.getId(), name) {
			@Override
			public Column get() {
				return column;
			}
		};
	}

	private static String render(Field<?> field) {
		return DSL.using(SQLDialect.POSTGRES).renderInlined(DSL.select(field)).substring("select ".length()).toLowerCase(Locale.ROOT);
	}
}
