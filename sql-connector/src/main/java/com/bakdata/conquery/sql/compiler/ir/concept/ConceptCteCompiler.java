package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStepJoiner;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectCompiler;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Builds concept-level CTEs from connector compiler IR. */
@UtilityClass
public class ConceptCteCompiler {

	public static QueryStep compileConcept(ConceptCteInput input) {
		Optional<QueryStep> intervalPackingSelects = Optional.empty();
		if (input.tables().isRequiredStep(ConceptCteStep.INTERVAL_PACKING_SELECTS)) {
			// TODO Derive the optional branch from the resolved selects instead of retaining duplicate CTE-graph state.
			QueryStep eventDateSelects = IntervalPackingSelectCompiler.compile(
					input.predecessor(),
					input.sqlSelects().stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
					input.tables()
			);
			intervalPackingSelects = Optional.of(eventDateSelects);
		}

		return compileUniversalSelects(
				input.predecessor(),
				input.sqlSelects(),
				intervalPackingSelects,
				input.tables(),
				input.negate()
		);
	}

	public static QueryStep compileUniversalSelects(
			QueryStep predecessor,
			List<ConceptSqlSelects> conceptSelects,
			Optional<QueryStep> intervalPackingSelects,
			SqlTables tables,
			boolean negate
	) {
		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(predecessor);
		conceptSelects.stream()
				.map(ConceptSqlSelects::getAdditionalPredecessor)
				.flatMap(Optional::stream)
				.forEach(queriesToJoin::add);
		intervalPackingSelects.ifPresent(queriesToJoin::add);

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		Optional<ColumnDateRange> validityDate = predecessorSelects.getValidityDate();
		SqlIdColumns ids = predecessorSelects.getIds();
		List<SqlSelect> allConceptSelects = Stream.concat(
				conceptSelects.stream().flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream()),
				predecessorSelects.getSqlSelects().stream().map(SqlSelect::connectorAggregate)
		).toList();

		Selects finalSelects = Selects.builder()
				.ids(ids)
				.stratificationDate(predecessorSelects.getStratificationDate())
				.validityDate(validityDate)
				.sqlSelects(allConceptSelects)
				.build();
		TableLike<Record> joinedTable = QueryStepJoiner.join(queriesToJoin, JoinMode.INNER);

		List<Field<?>> groupByFields = Stream.concat(
				finalSelects.nonExplicitSelects().stream(),
				finalSelects.getSqlSelects().stream()
						.filter(Predicate.not(SqlSelect::isUniversal))
						.flatMap(sqlSelect -> sqlSelect.toFields().stream())
		).toList();

		return QueryStep.builder()
				.cteName(tables.cteName(ConceptCteStep.UNIVERSAL_SELECTS))
				.selects(finalSelects)
				.fromTable(joinedTable)
				.groupBy(groupByFields)
				.predecessors(queriesToJoin)
				.negate(negate)
				.build();
	}
}
