package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Locale;

import com.bakdata.conquery.sql.translate.dialect.clickhouse.ClickhouseTranslatorDialect;
import com.bakdata.conquery.sql.translate.dialect.hana.HanaTranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaSqlFunctionProvider;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class SqlExtractionDialectTest {

	@Test
	void shouldKeepClickhouseRangeBoundsNullableAfterCoalesce() {
		var dialect = new ClickhouseDialectBundle();
		var range = dialect.getTranslatorDialect().dateRange(DSL.field(DSL.name("events", "start"), Date.class),
				DSL.field(DSL.name("events", "end"), Date.class));
		// ClickHouse otherwise replaces missing outer-join values with the epoch date.
		for (var bound : range.toFields()) {
			String sql = DSL.using(SQLDialect.CLICKHOUSE).renderInlined(bound).toLowerCase(Locale.ROOT);
			assertTrue(sql.contains("::nullable(date32)"), sql);
		}
	}

	@Test
	void shouldExposeFrameworkNeutralDialectCapabilities() {
		var clickhouse = new ClickhouseDialectBundle();
		var hana = new HanaDialectBundle();

		assertInstanceOf(ClickhouseTranslatorDialect.class, clickhouse.getTranslatorDialect());
		assertInstanceOf(HanaTranslatorDialect.class, hana.getTranslatorDialect());
		assertInstanceOf(ClickhouseFunctionProvider.class, clickhouse.getFunctionProvider());
		assertInstanceOf(HanaSqlFunctionProvider.class, hana.getFunctionProvider());
	}

	@Test
	void shouldPreserveClickhouseFormDateEmptinessCondition() {
		var dialect = new ClickhouseDialectBundle();
		ColumnDateRange range = ColumnDateRange.of(
				DSL.field(DSL.name("events", "start"), Date.class),
				DSL.field(DSL.name("events", "end"), Date.class));

		assertEquals(
				DSL.using(SQLDialect.CLICKHOUSE).renderInlined(
						dialect.getFunctionProvider().isNotEmptyDateRange(range)),
				DSL.using(SQLDialect.CLICKHOUSE).renderInlined(dialect.getTranslatorDialect().isNotEmptyDateRange(range))
		);
	}
}
