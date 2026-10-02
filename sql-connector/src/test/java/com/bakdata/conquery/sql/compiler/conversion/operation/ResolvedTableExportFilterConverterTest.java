package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.operation.BuiltInFilters;
import com.bakdata.conquery.sql.model.range.NumberRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ResolvedTableExportFilterConverterTest {

	private static final SqlTable TABLE = SqlTable.of("table", "events");
	private static final ResolvedColumn VALUE = new ResolvedColumn(
			"value", TABLE, "value", ColumnType.DECIMAL, true);
	private static final ResolvedColumn SUBTRACT = new ResolvedColumn(
			"subtract", TABLE, "subtract", ColumnType.DECIMAL, true);
	private static final NumberRange RANGE = new NumberRange(
			Optional.of(BigDecimal.ONE), Optional.of(BigDecimal.TEN));
	private final ResolvedTableExportFilterConverter converter = new ResolvedTableExportFilterConverter();

	@Test
	void shouldApplySubtractionToIndividualRows() {
		var filter = new BuiltInFilters.AggregationRange("sum",
				new BuiltInAggregations.Sum(VALUE, Optional.of(SUBTRACT), List.of()), RANGE);

		String sql = DSL.using(SQLDialect.DEFAULT).renderInlined(
				converter.convert(filter, new HanaCompilerDialect()));

		assertEquals("((\"events\".\"value\" - \"events\".\"subtract\") >= 1 and "
				+ "(\"events\".\"value\" - \"events\".\"subtract\") <= 10)", sql);
	}

	@Test
	void shouldTreatCountAsOneForEachExportedRow() {
		var filter = new BuiltInFilters.AggregationRange("count",
				new BuiltInAggregations.Count(VALUE, List.of()), RANGE);

		String sql = DSL.using(SQLDialect.DEFAULT).renderInlined(
				converter.convert(filter, new HanaCompilerDialect()));

		assertEquals("(1 >= 1 and 1 <= 10)", sql);
	}
}
