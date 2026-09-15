package com.bakdata.conquery.sql.compiler.ir.concept;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.condition.ConditionWrappingWhereCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class ConnectorCteCompilerTest {

	private static final String PREDECESSOR_NAME = "predecessor";

	@Test
	void shouldCompilePreprocessingAgainstConnectorTable() {
		PreprocessingCteInput input = preprocessingInput(Optional.empty());

		QueryStep result = ConnectorCteCompiler.compilePreprocessing(input).build();

		assertEquals(List.of(name("score")), result.getSelects().getSqlSelects().stream()
				.flatMap(sqlSelect -> sqlSelect.toFields().stream())
				.map(Field::getQualifiedName)
				.toList());
		assertEquals(3, result.getConditions().size());
		String sql = render(result);
		assertTrue(sql.contains("from \"events\""));
		assertTrue(sql.contains("\"events\".\"status\" = 'active'"));
		assertTrue(sql.contains("\"events\".\"score\" > 0"));
		assertTrue(sql.contains("\"events\".\"secondary\" is not null"));
	}

	@Test
	void shouldCompilePreprocessingWithStratificationJoin() {
		QueryStep stratificationTable = QueryStep.builder()
				.cteName("stratification")
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.stratificationDate(Optional.of(ColumnDateRange.of(
								field(name("stratification_start"), Date.class),
								field(name("stratification_end"), Date.class)
						)))
						.build())
				.build();
		PreprocessingCteInput input = preprocessingInput(Optional.of(stratificationTable));

		QueryStep result = ConnectorCteCompiler.compilePreprocessing(input).build();

		assertEquals(name("stratification", "person"), result.getSelects().getIds().getPrimaryColumn().getQualifiedName());
		assertEquals(
				List.of(name("stratification", "stratification_start"), name("stratification", "stratification_end")),
				qualifiedNames(result.getSelects().getStratificationDate().orElseThrow())
		);
		String sql = render(result);
		assertTrue(sql.contains("from \"events\" join \"stratification\""));
		assertTrue(sql.contains("\"stratification\".\"person\" = \"events\".\"person\""));
		assertTrue(sql.contains("\"stratification\".\"stratification_start\" < \"events\".\"valid_to\""));
		assertTrue(sql.contains("\"stratification\".\"stratification_end\" > \"events\".\"valid_from\""));
	}

	@Test
	void shouldCompileAggregationSelectsAndGroupByPredecessorDimensions() {
		FieldWrapper<Integer> aggregation = new FieldWrapper<>(field(name("aggregated"), Integer.class));
		QueryStep result = ConnectorCteCompiler.compileAggregationSelect(
				predecessor(),
				List.of(ConnectorSqlSelects.builder().aggregationSelect(aggregation).build())
		).build();

		assertEquals(List.of(aggregation), result.getSelects().getSqlSelects());
		assertEquals(
				List.of(name(PREDECESSOR_NAME, "person"), name(PREDECESSOR_NAME, "stratification_start"), name(PREDECESSOR_NAME, "stratification_end")),
				result.getGroupBy().stream().map(Field::getQualifiedName).toList()
		);
	}

	@Test
	void shouldCompileFinalSelectsAndGroupFilters() {
		QueryStep predecessor = predecessor();
		FieldWrapper<Integer> extracted = new FieldWrapper<>(field(name("metric"), Integer.class));
		ExistsSqlSelect universal = ExistsSqlSelect.withAlias("exists");
		Condition groupFilter = field(name("count"), Integer.class).greaterThan(2);
		ConnectorSqlSelects sqlSelects = ConnectorSqlSelects.builder()
				.finalSelect(extracted)
				.finalSelect(universal)
				.build();
		SqlFilters sqlFilter = new SqlFilters(
				ConnectorSqlSelects.none(),
				WhereClauses.builder()
						.groupFilter(new ConditionWrappingWhereCondition(groupFilter))
						.build()
		);

		QueryStep result = ConnectorCteCompiler.compileAggregationFilter(
				predecessor,
				List.of(sqlSelects),
				List.of(sqlFilter)
		).build();

		List<SqlSelect> resultSelects = result.getSelects().getSqlSelects();
		assertEquals(
				name(PREDECESSOR_NAME, "metric"),
				resultSelects.getFirst().toFields().getFirst().getQualifiedName()
		);
		assertSame(universal, resultSelects.getLast());
		assertEquals(List.of(groupFilter), result.getConditions());

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		assertEquals(predecessorSelects.getIds().toFields(), result.getSelects().getIds().toFields());
		assertEquals(
				qualifiedNames(predecessorSelects.getStratificationDate().orElseThrow()),
				qualifiedNames(result.getSelects().getStratificationDate().orElseThrow())
		);
		assertEquals(
				qualifiedNames(predecessorSelects.getValidityDate().orElseThrow()),
				qualifiedNames(result.getSelects().getValidityDate().orElseThrow())
		);
	}

	private static List<Name> qualifiedNames(ColumnDateRange range) {
		return range.toFields().stream().map(Field::getQualifiedName).toList();
	}

	private static PreprocessingCteInput preprocessingInput(Optional<QueryStep> stratificationTable) {
		SqlIdColumns ids = new SqlIdColumns(
				field(name("events", "person"), String.class),
				field(name("events", "secondary"), String.class)
		).withAlias();
		ColumnDateRange rawValidityDate = ColumnDateRange.of(
				field(name("events", "valid_from"), Date.class),
				field(name("events", "valid_to"), Date.class)
		);
		ConnectorSqlSelects sqlSelects = ConnectorSqlSelects.builder()
				.preprocessingSelect(new FieldWrapper<>(field(name("events", "score"), Integer.class).as("score")))
				.build();
		SqlFilters sqlFilters = new SqlFilters(
				ConnectorSqlSelects.none(),
				WhereClauses.builder()
						.preprocessingCondition(new ConditionWrappingWhereCondition(
								field(name("events", "status"), String.class).eq("active")
						))
						.eventFilter(new ConditionWrappingWhereCondition(
								field(name("events", "score"), Integer.class).greaterThan(0)
						))
						.groupFilter(new ConditionWrappingWhereCondition(
								field(name("events", "score"), Integer.class).lessThan(10)
						))
						.build()
		);
		return new PreprocessingCteInput(
				"events",
				ids,
				rawValidityDate,
				rawValidityDate.asValidityDateRange("connector"),
				List.of(sqlSelects),
				List.of(sqlFilters),
				stratificationTable
		);
	}

	private static String render(QueryStep queryStep) {
		return DSL.using(SQLDialect.POSTGRES)
				.select(queryStep.getSelects().all())
				.from(queryStep.getFromTables().getFirst())
				.where(queryStep.getConditions())
				.getSQL(ParamType.INLINED)
				.toLowerCase(Locale.ROOT);
	}

	private static QueryStep predecessor() {
		return QueryStep.builder()
				.cteName(PREDECESSOR_NAME)
				.selects(Selects.builder()
						.ids(new SqlIdColumns(field(name("person"), String.class)))
						.stratificationDate(Optional.of(ColumnDateRange.of(
								field(name("stratification_start"), Date.class),
								field(name("stratification_end"), Date.class)
						)))
						.validityDate(Optional.of(ColumnDateRange.of(
								field(name("validity_start"), Date.class),
								field(name("validity_end"), Date.class)
						)))
						.build())
				.build();
	}
}
