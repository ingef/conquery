package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;

/** Builds connector-level concept CTE bodies from compiler IR. */
@UtilityClass
public class ConnectorCteCompiler {

	public static QueryStep.QueryStepBuilder compileAggregationSelect(
			QueryStep predecessor,
			List<ConnectorSqlSelects> sqlSelects
	) {
		List<SqlSelect> aggregationSelects = sqlSelects.stream()
				.flatMap(selects -> selects.getAggregationSelects().stream())
				.toList();

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		SqlIdColumns ids = predecessorSelects.getIds();
		Optional<ColumnDateRange> stratificationDate = predecessorSelects.getStratificationDate();
		Selects selects = Selects.builder()
				.ids(ids)
				.stratificationDate(stratificationDate)
				.sqlSelects(aggregationSelects)
				.build();

		List<Field<?>> groupByFields = Stream.concat(
				ids.toFields().stream(),
				stratificationDate.stream().flatMap(range -> range.toFields().stream())
		).toList();

		return QueryStep.builder()
				.selects(selects)
				.groupBy(groupByFields);
	}

	public static QueryStep.QueryStepBuilder compileAggregationFilter(
			QueryStep predecessor,
			List<ConnectorSqlSelects> sqlSelects,
			List<SqlFilters> sqlFilters
	) {
		Selects predecessorSelects = predecessor.getQualifiedSelects();
		List<SqlSelect> finalSelects = sqlSelects.stream()
				.flatMap(selects -> selects.getFinalSelects().stream())
				.map(sqlSelect -> qualifyUnlessUniversal(sqlSelect, predecessor.getCteName()))
				.toList();
		Selects selects = Selects.builder()
				.ids(predecessorSelects.getIds())
				.stratificationDate(predecessorSelects.getStratificationDate())
				.validityDate(predecessorSelects.getValidityDate())
				.sqlSelects(finalSelects)
				.build();

		List<Condition> conditions = sqlFilters.stream()
				.flatMap(sqlFilter -> sqlFilter.getWhereClauses().getGroupFilters().stream())
				.map(WhereCondition::condition)
				.toList();

		return QueryStep.builder()
				.selects(selects)
				.conditions(conditions);
	}

	private static SqlSelect qualifyUnlessUniversal(SqlSelect sqlSelect, String predecessorName) {
		return sqlSelect.isUniversal() ? sqlSelect : sqlSelect.qualify(predecessorName);
	}
}
