package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

class PreprocessingCte extends ConnectorCte {

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
		// All event-level where clauses are applied directly to the connector table while preprocessing.
		List<Condition> conditions = new ArrayList<>();

		for (SqlFilters sqlFilter : tableContext.getSqlFilters()) {
			for (WhereCondition whereCondition : sqlFilter.getWhereClauses().getPreprocessingConditions()) {
				conditions.add(whereCondition.condition());
			}
			for (WhereCondition whereCondition : sqlFilter.getWhereClauses().getEventFilters()) {
				conditions.add(whereCondition.condition());
			}
		}

		// The aliased secondary ID is selected in this CTE, so its root-table expression must be used in the WHERE clause.
		tableContext.getIds()
					.getPredecessor()
					.flatMap(SqlIdColumns::getSecondaryId)
					.ifPresent(secondaryId -> conditions.add(secondaryId.isNotNull()));

		QueryStep.QueryStepBuilder builder = QueryStep.builder()
													  .selects(preprocessingSelects)
													  .conditions(conditions);

		if (tableContext.getConversionContext().isWithStratification()) {
			return joinWithStratificationTable(forPreprocessing, conditions, tableContext);
		}

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
		ColumnDateRange stratificationDate = stratificationSelects.getStratificationDate().orElseThrow(() -> new IllegalStateException(
				"Stratification table must provide a stratification date"
		));
		// Both expressions are available from the joined source tables; do not reference aliases produced by this SELECT.
		conditions.add(functionProvider.dateRestriction(stratificationDate, tableContext.getRawValidityDate()));

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
