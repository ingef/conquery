package com.bakdata.conquery.sql.translate.query.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.query.QueryStepJoiner;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingSelectTranslator;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

/** Builds concept-level CTEs from connector translate IR. */
@UtilityClass
public class ConceptCteTranslator {

	public static QueryStep translateConcept(ConceptCteInput input) {
		Optional<QueryStep> intervalPackingSelects = Optional.empty();
		if (input.tables().isRequiredStep(ConceptCteStep.INTERVAL_PACKING_SELECTS)) {
			// TODO Derive the optional branch from the resolved selects instead of retaining duplicate CTE-graph state.
			QueryStep eventDateSelects = IntervalPackingSelectTranslator.translate(
					input.predecessor(),
					input.sqlSelects().stream().flatMap(selects -> selects.getEventDateSelects().stream()).toList(),
					input.tables()
			);
			intervalPackingSelects = Optional.of(eventDateSelects);
		}

		return translateUniversalSelects(
				input.predecessor(),
				input.sqlSelects(),
				intervalPackingSelects,
				input.tables(),
				input.negate(),
				input.blockValidityDate()
		);
	}

	public static QueryStep translateUniversalSelects(
			QueryStep predecessor,
			List<ConceptSqlSelects> conceptSelects,
			Optional<QueryStep> intervalPackingSelects,
			SqlTables tables,
			boolean negate,
			boolean blockValidityDate
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
		if (blockValidityDate) {
			finalSelects = finalSelects.blockValidityDate();
		}
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
