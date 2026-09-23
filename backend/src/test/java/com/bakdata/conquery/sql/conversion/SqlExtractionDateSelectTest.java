package com.bakdata.conquery.sql.conversion;

import static com.bakdata.conquery.sql.conversion.SqlExtractionAggregationTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDateUnionSelect;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.aggregator.DateDistanceSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.aggregator.FlagSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.DateUnionSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.EventDateUnionSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.EventDurationSumSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class SqlExtractionDateSelectTest {
	@Test
	void shouldUsePerRowStratificationEndForDistance() {
		var base = context();
		var dates = ColumnDateRange.of(DSL.field(DSL.name("start"), Date.class), DSL.field(DSL.name("end"), Date.class)).as("window");
		var step = com.bakdata.conquery.sql.compiler.ir.QueryStep.builder().cteName("stratification")
				.selects(com.bakdata.conquery.sql.compiler.ir.Selects.builder().ids(base.getIds()).stratificationDate(Optional.of(dates)).build()).build();
		var context = SelectContext.create(base.getIds(), base.getValidityDate(), base.getTables(), base.getConversionContext().withStratificationTable(step));
		var select = new DateDistanceSelect(column("date", MajorTypeId.DATE));
		select.setName("distance");
		var result = new DateDistanceSqlAggregator().connectorSelect(select, context);
		String expression = render(result.getPreprocessingSelects().getFirst().toFields().getFirst());
		assertTrue(expression.contains("\"stratification\".\"window_end\""));
		assertTrue(expression.contains("-1"));
	}
	@Test
	void shouldPackDateUnionBeforeAggregation() {
		var select = new DateUnionSelect();
		select.setName("dates");
		select.setStartColumn(column("start", MajorTypeId.DATE));
		select.setEndColumn(column("end", MajorTypeId.DATE));
		var result = new DateUnionSelectConverter().connectorSelect(select, context());
		assertEquals(2, result.getPreprocessingSelects().size());
		assertEquals("dates-1-interval_packing_selects", result.getAdditionalPredecessor().orElseThrow().getCteName());
		assertTrue(result.getAggregationSelects().isEmpty());
		assertEquals("\"aggregation\".\"dates-1\"", render(result.getFinalSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldKeepEventDatesInEventAggregation() {
		var base = context();
		var dates = ColumnDateRange.of(DSL.field(DSL.name("start"), Date.class), DSL.field(DSL.name("end"), Date.class));
		var context = SelectContext.create(base.getIds(), Optional.of(dates), base.getTables(), base.getConversionContext());
		var union = new EventDateUnionSelect();
		union.setName("union");
		var unionResult = new EventDateUnionSelectConverter().connectorSelect(union, context);
		assertEquals(1, unionResult.getEventDateSelects().size());
		assertTrue(unionResult.getAggregationSelects().isEmpty());
		var duration = EventDurationSumSelect.create("duration");
		duration.setName("duration");
		var durationResult = new EventDurationSumSelectConverter().connectorSelect(duration, context);
		assertEquals(1, durationResult.getEventDateSelects().size());
		String expression = render(durationResult.getEventDateSelects().getFirst().toFields().getFirst());
		assertTrue(expression.contains("sum(case when"));
		assertTrue(expression.contains("then null"));
	}

	@Test
	void shouldCalculateMinimumDistanceToFrozenCurrentDate() {
		var base = context();
		var conversion = base.getConversionContext().withClock(Clock.fixed(Instant.parse("2020-06-15T12:00:00Z"), ZoneOffset.UTC));
		var context = SelectContext.create(base.getIds(), base.getValidityDate(), base.getTables(), conversion);
		var select = new DateDistanceSelect(column("date", MajorTypeId.DATE));
		select.setName("distance");
		var result = new DateDistanceSqlAggregator().connectorSelect(select, context);
		assertTrue(render(result.getPreprocessingSelects().getFirst().toFields().getFirst()).contains("2020-06-15"));
		assertEquals("min(\"preprocessing\".\"distance-1\") as \"distance-1\"", render(result.getAggregationSelects().getFirst().toFields().getFirst()));
	}

	@Test
	void shouldKeepFlagNamesAndEmptyStringFallback() {
		var select = new FlagSelect(Map.of("A", column("a", MajorTypeId.BOOLEAN), "B", column("b", MajorTypeId.BOOLEAN)));
		select.setName("flags");
		var result = new FlagSqlAggregator().connectorSelect(select, context());
		assertEquals(2, result.getPreprocessingSelects().size());
		String expression = render(result.getAggregationSelects().getFirst().toFields().getFirst());
		assertTrue(expression.contains("then 'a'"));
		assertTrue(expression.contains("then 'b'"));
		assertTrue(expression.contains("else ''"));
	}
}
