package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.util.Locale;

import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaDialectBundle;
import com.bakdata.conquery.sql.compiler.dialect.clickhouse.ClickhouseCompilerDialect;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class SqlExtractionDialectTest {

	@Test
	void shouldKeepClickhouseRangeBoundsNullableAfterCoalesce() {
		var dialect = new ClickhouseDialectBundle();
		var range = dialect.dateRange(DSL.field(DSL.name("events", "start"), Date.class),
				DSL.field(DSL.name("events", "end"), Date.class));
		// ClickHouse otherwise replaces missing outer-join values with the epoch date.
		for (var bound : range.toFields()) {
			String sql = DSL.using(SQLDialect.CLICKHOUSE).renderInlined(bound).toLowerCase(Locale.ROOT);
			assertTrue(sql.contains("::nullable(date32)"), sql);
		}
	}

	@Test
	void shouldUseFrameworkNeutralDialectCapabilitiesFromLegacyBundles() {
		var clickhouse = new ClickhouseDialectBundle();
		var hana = new HanaDialectBundle();

		assertInstanceOf(ClickhouseCompilerDialect.class, clickhouse.getCompilerDialect());
		assertInstanceOf(HanaCompilerDialect.class, hana.getCompilerDialect());
		assertEquals(
				DSL.using(SQLDialect.CLICKHOUSE).renderInlined(clickhouse.getFunctionProvider().getMinDateExpression()),
				DSL.using(SQLDialect.CLICKHOUSE).renderInlined(clickhouse.minimumDate()));
		assertEquals(
				DSL.using(SQLDialect.DEFAULT).renderInlined(hana.getFunctionProvider().getMaxDateExpression()),
				DSL.using(SQLDialect.DEFAULT).renderInlined(hana.maximumDate()));
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
				DSL.using(SQLDialect.CLICKHOUSE).renderInlined(dialect.isNotEmptyDateRange(range))
		);
	}
}
