package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
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

	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> forPreprocessing = tableContext.allSqlSelects().stream()
													   .flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
													   .toList();

		Selects preprocessingSelects = Selects.builder()
											  .ids(tableContext.getIds())
											  .validityDate(tableContext.getValidityDate())
											  .sqlSelects(forPreprocessing)
											  .build();

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = tableContext.getSqlFilters().stream()
												 .flatMap(sqlFilter -> sqlFilter.getWhereClauses().getPreprocessingConditions().stream())
												 .map(WhereCondition::condition)
												 .toList();

		QueryStep.QueryStepBuilder builder = QueryStep.builder()
													  .selects(preprocessingSelects)
													  .conditions(conditions);

		if (!tableContext.getConversionContext().isWithStratification()) {
			TableLike<Record> rootTable = QueryStep.toTableLike(tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING));
			return builder.fromTable(rootTable);
		}

		return joinWithStratificationTable(forPreprocessing, conditions, tableContext);
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
		SqlFunctionProvider functionProvider = tableContext.getConversionContext().getSqlDialect().getFunctionProvider();
		Table<Record> connectorTable = DSL.table(DSL.name(tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING)));
		TableLike<Record> joinedTable = functionProvider.innerJoin(connectorTable, stratificationTable, idConditions);

		Selects selects = Selects.builder()
								 .ids(stratificationSelects.getIds())
								 .validityDate(tableContext.getValidityDate())
								 .stratificationDate(stratificationSelects.getStratificationDate())
								 .sqlSelects(preprocessingSelects)
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(joinedTable)
						.conditions(conditions);
	}

}
