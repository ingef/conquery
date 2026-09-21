package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Locale;

import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
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
}
