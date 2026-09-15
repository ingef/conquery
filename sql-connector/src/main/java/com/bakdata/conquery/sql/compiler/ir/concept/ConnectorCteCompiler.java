package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStepJoiner;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import com.bakdata.conquery.sql.compiler.ir.interval.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingContext;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectCompiler;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/** Builds connector-level concept CTE bodies from compiler IR. */
@UtilityClass
public class ConnectorCteCompiler {
	private static final List<ConceptCteStep> CONNECTOR_PIPELINE = List.of(
			ConceptCteStep.PREPROCESSING,
			ConceptCteStep.AGGREGATION_SELECT,
			ConceptCteStep.JOIN_BRANCHES,
			ConceptCteStep.AGGREGATION_FILTER
	);

	public static Optional<QueryStep> compileConnector(ConnectorCtePipelineInput input) {
		Optional<QueryStep> previous = Optional.empty();
		for (ConceptCteStep cteStep : CONNECTOR_PIPELINE) {
			if (!input.tables().isRequiredStep(cteStep)) {
				continue;
			}
			QueryStep.QueryStepBuilder builder = compileConnectorStep(input, cteStep, previous)
					.cteName(input.tables().cteName(cteStep));
			previous = Optional.of(completeStep(builder, previous));
		}
		return previous;
	}

	public static QueryStep.QueryStepBuilder compilePreprocessing(PreprocessingCteInput input) {
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
					.selects(selects)
					.fromTable(QueryStep.toTableLike(input.sourceTable()))
					.conditions(conditions);
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

		Table<Record> connectorTable = DSL.table(DSL.name(input.sourceTable()));
		TableLike<Record> joinedTable = connectorTable.innerJoin(stratificationTable)
				.on(idConditions.toArray(Condition[]::new));
		Selects selects = Selects.builder()
				.ids(stratificationSelects.getIds())
				.validityDate(Optional.of(input.validityDate()))
				.stratificationDate(stratificationSelects.getStratificationDate())
				.sqlSelects(preprocessingSelects)
				.build();

		return QueryStep.builder()
				.selects(selects)
				.fromTable(joinedTable)
				.conditions(conditions);
	}

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

	public static QueryStep.QueryStepBuilder compileJoinBranches(JoinBranchesCteInput input) {
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

			QueryStep intervalPackingSelects = IntervalPackingSelectCompiler.compile(
					lastIntervalPackingStep,
					input.eventDateSelects(),
					input.tables()
			);
			if (intervalPackingSelects != lastIntervalPackingStep) {
				queriesToJoin.add(intervalPackingSelects);
			}

			if (input.excludedFromTimeAggregation()) {
				validityDate = Optional.empty();
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
				.selects(selects)
				.fromTable(QueryStepJoiner.join(queriesToJoin, JoinMode.FULL_OUTER))
				.predecessors(queriesToJoin);
	}

	private static QueryStep.QueryStepBuilder compileConnectorStep(
			ConnectorCtePipelineInput input,
			ConceptCteStep cteStep,
			Optional<QueryStep> previous
	) {
		if (cteStep == ConceptCteStep.PREPROCESSING) {
			// TODO this looks like it can be unwinded
			return compilePreprocessing(input.preprocessing());
		}

		QueryStep predecessor = previous.orElseThrow(() -> new IllegalStateException(
				"Connector CTE %s requires a predecessor".formatted(cteStep)
		));
		return switch (cteStep) {
			case AGGREGATION_SELECT -> compileAggregationSelect(predecessor, input.preprocessing().sqlSelects());
			case JOIN_BRANCHES -> compileJoinBranches(new JoinBranchesCteInput(
					predecessor,
					input.preprocessing().validityDate(),
					input.tables(),
					input.withIntervalPacking(),
					input.excludedFromTimeAggregation(),
					input.eventDateSelects(),
					input.additionalPredecessors()
			));
			case AGGREGATION_FILTER -> compileAggregationFilter(
					predecessor,
					input.preprocessing().sqlSelects(),
					input.preprocessing().sqlFilters()
			);
			default -> throw new IllegalArgumentException("Not a connector pipeline step: %s".formatted(cteStep));
		};
	}

	private static QueryStep completeStep(QueryStep.QueryStepBuilder builder, Optional<QueryStep> previous) {
		if (previous.isEmpty()) {
			return builder.predecessors(List.of()).build();
		}

		QueryStep partialStep = builder.build();
		if (partialStep.getFromTables().isEmpty() && partialStep.getPredecessors().isEmpty()) {
			QueryStep predecessor = previous.orElseThrow();
			return builder
					.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
					.predecessors(List.of(predecessor))
					.build();
		}
		return partialStep;
	}

	private static SqlSelect qualifyUnlessUniversal(SqlSelect sqlSelect, String predecessorName) {
		return sqlSelect.isUniversal() ? sqlSelect : sqlSelect.qualify(predecessorName);
	}
}
