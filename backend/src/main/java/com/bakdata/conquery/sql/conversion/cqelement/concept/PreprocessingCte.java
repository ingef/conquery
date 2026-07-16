package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

class PreprocessingCte extends ConnectorCte {

	public static List<Condition> collectEventFilterConditions(CQTableContext tableContext) {

		List<Condition> eventFilterConditions = tableContext.getSqlFilters().stream()
															.flatMap(conceptFilter -> conceptFilter.getWhereClauses().getEventFilters().stream())
															.map(WhereCondition::condition)
															.toList();

		if (tableContext.getConversionContext().isWithStratification()) {
			return addStratificationCondition(eventFilterConditions, tableContext);
		}
		return eventFilterConditions;
	}

	public static List<Condition> addStratificationCondition(List<Condition> eventFilterConditions, CQTableContext tableContext) {
		Selects previousSelects = tableContext.getPrevious().getQualifiedSelects();
		Preconditions.checkArgument(
				previousSelects.getStratificationDate().isPresent() && previousSelects.getValidityDate().isPresent(),
				"Can't apply stratification for table %s".formatted(tableContext.getConnectorTables().getRootTable())
		);

		// we filter every entry where stratification date range and validity date range do not overlap
		SqlFunctionProvider functionProvider = tableContext.getFunctionProvider();
		ColumnDateRange stratificationDate = previousSelects.getStratificationDate().get();
		ColumnDateRange validityDate = previousSelects.getValidityDate().get();
		Condition stratificationCondition = functionProvider.dateRestriction(stratificationDate, validityDate);

		return Stream.concat(Stream.of(stratificationCondition), eventFilterConditions.stream()).toList();
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.PREPROCESSING;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forPreprocessing = tableContext.allSqlSelects().stream()
													   .flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
													   .toList();

		Selects preprocessingSelects = Selects.builder()
											  .ids(tableContext.getIds())
											  .validityDate(Optional.of(tableContext.getValidityDate()))
											  .sqlSelects(forPreprocessing)
											  .build();

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = new ArrayList<>();

		for (SqlFilters sqlFilter : tableContext.getSqlFilters()) {
			for (WhereCondition whereCondition : sqlFilter.getWhereClauses().getPreprocessingConditions()) {
				conditions.add(whereCondition.condition());
			}
		}

		conditions.addAll(collectEventFilterConditions(tableContext));

		if (tableContext.getIds().getSecondaryId().isPresent()) {
			conditions.add(tableContext.getIds().getSecondaryId().get().isNotNull());
		}

		if (tableContext.getConversionContext().isWithStratification()) {
			return joinWithStratificationTable(forPreprocessing, conditions, tableContext);
		}

		QueryStep.QueryStepBuilder builder = QueryStep.builder()
				.selects(preprocessingSelects)
				.conditions(conditions);

		TableLike<Record> rootTable = QueryStep.toTableLike(tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING));
		return builder.fromTable(rootTable);

	}


	private static QueryStep.QueryStepBuilder joinWithStratificationTable(
			List<SqlSelect> preprocessingSelects,
			List<Condition> conditions,
			CQTableContext tableContext
	) {
		QueryStep stratificationTableCte = tableContext.getConversionContext().getStratificationTable();
		Table<Record> stratificationTable = DSL.table(DSL.name(stratificationTableCte.getCteName()));

		Selects stratificationSelects = stratificationTableCte.getQualifiedSelects();
		SqlIdColumns stratificationIds = stratificationSelects.getIds();
		SqlIdColumns rootTableIds = tableContext.getIds().getPredecessor().orElseThrow(() -> new IllegalStateException(
				"Id's should have been qualified during conversion and thus have a predecessor")
		);
		List<Condition> idConditions = stratificationIds.join(rootTableIds);

		// join full stratification with connector table on all ID's from prerequisite query
		SqlFunctionProvider functionProvider = tableContext.getConversionContext().getFunctionProvider();
		Table<Record> connectorTable = DSL.table(DSL.name(tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING)));
		TableLike<Record> joinedTable = functionProvider.innerJoin(connectorTable, stratificationTable, idConditions);

		Selects selects = Selects.builder()
								 .ids(stratificationSelects.getIds())
								 .validityDate(Optional.of(tableContext.getValidityDate()))
								 .stratificationDate(stratificationSelects.getStratificationDate())
								 .sqlSelects(preprocessingSelects)
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(joinedTable)
						.conditions(conditions);
	}

}
