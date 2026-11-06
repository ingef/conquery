package com.bakdata.conquery.sql.translate.query.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.translate.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.query.QueryStepJoiner;
import com.bakdata.conquery.sql.translate.query.interval.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingContext;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingSelectTranslator;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Builds connector-level concept CTE bodies from translate IR. */
@UtilityClass
public class ConnectorCteTranslator {
	private static final List<ConceptCteStep> CONNECTOR_PIPELINE = List.of(
			ConceptCteStep.PREPROCESSING,
			ConceptCteStep.AGGREGATION_SELECT,
			ConceptCteStep.JOIN_BRANCHES,
			ConceptCteStep.AGGREGATION_FILTER
	);

	public static Optional<QueryStep> translateConnector(ConnectorCtePipelineInput input) {
		Optional<QueryStep> previous = Optional.empty();
		for (ConceptCteStep cteStep : CONNECTOR_PIPELINE) {
			if (!input.tables().isRequiredStep(cteStep)) {
				continue;
			}
			previous = Optional.of(translateConnectorStep(input, cteStep, previous));
		}
		return previous;
	}

	static QueryStep translatePreprocessing(PreprocessingCteInput input, String cteName) {
		List<SqlSelect> preprocessingSelects = input.sqlSelects().stream()
				.flatMap(selects -> selects.getPreprocessingSelects().stream())
				.toList();
		List<Condition> conditions = input.sqlFilters().stream()
				.flatMap(sqlFilter -> Stream.concat(
						sqlFilter.getWhereClauses().getPreprocessingConditions().stream(),
						sqlFilter.getWhereClauses().getEventFilters().stream()
				))
				.map(WhereCondition::condition)
				.collect(Collectors.toCollection(ArrayList::new));

		input.ids()
				.getPredecessor()
				.flatMap(SqlIdColumns::getSecondaryId)
				.ifPresent(secondaryId -> conditions.add(secondaryId.isNotNull()));

		if (input.stratificationTable().isEmpty()) {
			Selects selects = Selects.builder()
					.ids(input.ids())
					.validityDate(Optional.of(input.validityDate()))
					.sqlSelects(preprocessingSelects)
					.build();
			return QueryStep.builder()
					.cteName(cteName)
					.selects(selects)
					.fromTable(input.sourceTable())
					.conditions(conditions)
					.predecessors(List.of())
					.build();
		}

		QueryStep stratificationTableCte = input.stratificationTable().orElseThrow();
		Table<Record> stratificationTable = DSL.table(DSL.name(stratificationTableCte.getCteName()));
		Selects stratificationSelects = stratificationTableCte.getQualifiedSelects();
		SqlIdColumns rootTableIds = input.ids().getPredecessor().orElseThrow(() -> new IllegalStateException(
				"IDs must retain their source expressions when preprocessing uses stratification"
		));
		List<Condition> idConditions = stratificationSelects.getIds().join(rootTableIds);
		ColumnDateRange stratificationDate = stratificationSelects.getStratificationDate().orElseThrow(() -> new IllegalStateException(
				"Stratification table must provide a stratification date"
		));
		conditions.add(new DateRestrictionCondition(stratificationDate, input.rawValidityDate()).condition());

		Table<Record> connectorTable = input.sourceTable();
		Table<Record> joinedTable = connectorTable.innerJoin(stratificationTable)
				.on(idConditions.toArray(Condition[]::new));
		Selects selects = Selects.builder()
				.ids(stratificationSelects.getIds())
				.validityDate(Optional.of(input.validityDate()))
				.stratificationDate(stratificationSelects.getStratificationDate())
				.sqlSelects(preprocessingSelects)
				.build();

		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(joinedTable)
				.conditions(conditions)
				.predecessors(List.of())
				.build();
	}

	static QueryStep translateAggregationSelect(
			QueryStep predecessor,
			List<ConnectorSqlSelects> sqlSelects,
			String cteName
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
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
				.groupBy(groupByFields)
				.predecessors(List.of(predecessor))
				.build();
	}

	static QueryStep translateAggregationFilter(
			QueryStep predecessor,
			List<ConnectorSqlSelects> sqlSelects,
			List<SqlFilters> sqlFilters,
			String cteName
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
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
				.conditions(conditions)
				.predecessors(List.of(predecessor))
				.build();
	}

	static QueryStep translateJoinBranches(JoinBranchesCteInput input, String cteName) {
		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(input.predecessor());

		Optional<ColumnDateRange> validityDate = Optional.empty();
		if (input.withIntervalPacking()) {
			IntervalPackingContext intervalPackingContext = IntervalPackingContext.builder()
					.ids(input.predecessor().getQualifiedSelects().getIds())
					.daterange(input.validityDate())
					.tables(input.tables())
					.build();
			QueryStep lastIntervalPackingStep = AnsiSqlIntervalPacker.aggregateAsValidityDate(intervalPackingContext);
			queriesToJoin.add(lastIntervalPackingStep);
			validityDate = lastIntervalPackingStep.getQualifiedSelects().getValidityDate();

			QueryStep intervalPackingSelects = IntervalPackingSelectTranslator.translate(
					lastIntervalPackingStep,
					input.eventDateSelects(),
					input.tables()
			);
			if (intervalPackingSelects != lastIntervalPackingStep) {
				queriesToJoin.add(intervalPackingSelects);
			}

		}

		queriesToJoin.addAll(input.additionalPredecessors());

		Selects selects = Selects.builder()
				.ids(QueryStepJoiner.coalesceIds(queriesToJoin))
				.stratificationDate(input.predecessor().getQualifiedSelects().getStratificationDate())
				.validityDate(validityDate)
				.sqlSelects(QueryStepJoiner.mergeSelects(queriesToJoin))
				.build();

		return QueryStep.builder()
				.cteName(cteName)
				.selects(selects)
				.fromTable(QueryStepJoiner.join(queriesToJoin, JoinMode.FULL_OUTER))
				.predecessors(queriesToJoin)
				.build();
	}

	private static QueryStep translateConnectorStep(
			ConnectorCtePipelineInput input,
			ConceptCteStep cteStep,
			Optional<QueryStep> previous
	) {
		String cteName = input.tables().cteName(cteStep);
		if (cteStep == ConceptCteStep.PREPROCESSING) {
			// TODO this looks like it can be unwinded
			return translatePreprocessing(input.preprocessing(), cteName);
		}

		QueryStep predecessor = previous.orElseThrow(() -> new IllegalStateException(
				"Connector CTE %s requires a predecessor".formatted(cteStep)
		));
		return switch (cteStep) {
			case AGGREGATION_SELECT -> translateAggregationSelect(predecessor, input.preprocessing().sqlSelects(), cteName);
			case JOIN_BRANCHES -> translateJoinBranches(new JoinBranchesCteInput(
					predecessor,
					input.preprocessing().validityDate(),
					input.tables(),
					input.withIntervalPacking(),
					input.eventDateSelects(),
					input.additionalPredecessors()
			), cteName);
			case AGGREGATION_FILTER -> translateAggregationFilter(
					predecessor,
					input.preprocessing().sqlSelects(),
					input.preprocessing().sqlFilters(),
					cteName
			);
			default -> throw new IllegalArgumentException("Not a connector pipeline step: %s".formatted(cteStep));
		};
	}

	private static SqlSelect qualifyUnlessUniversal(SqlSelect sqlSelect, String predecessorName) {
		return sqlSelect.isUniversal() ? sqlSelect : sqlSelect.qualify(predecessorName);
	}
}
