package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCteCompiler;
import com.bakdata.conquery.sql.compiler.ir.concept.PreprocessingCteInput;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

class PreprocessingCte extends ConnectorCte {

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.PREPROCESSING;
	}

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		PreprocessingCteInput input = new PreprocessingCteInput(
				createConceptJoin(tableContext),
				tableContext.getIds(),
				tableContext.getRawValidityDate(),
				tableContext.getValidityDate(),
				tableContext.allSqlSelects(),
				tableContext.getSqlFilters(),
				Optional.ofNullable(tableContext.getConversionContext().getStratificationTable())
		);
		return ConnectorCteCompiler.compilePreprocessing(input);
	}

	private static Table<?> createConceptJoin(CQTableContext tableContext) {
		Table<Record> connectorTable = DSL.table(DSL.name(tableContext.getConnectorTables().getPredecessor(ConceptCteStep.PREPROCESSING)));
		ConceptIdMapping mapping = tableContext.getConceptIdMapping();

		if (mapping.includesRoot(tableContext.getSelectedConceptElements()) && !tableContext.isResolveConceptIds()) {
			return connectorTable;
		}

		Condition joinCondition = mapping.joinCondition(mappingConnector(tableContext));
		Condition conceptFilterCondition = tableContext.getSelectedConceptElements().isEmpty()
				? DSL.noCondition()
				: mapping.resolvedId().in(mapping.includedLocalIds(tableContext.getSelectedConceptElements()));

		return tableContext.getConversionContext().getFunctionProvider().innerJoin(
				connectorTable,
				mapping.table(),
				List.of(joinCondition.and(conceptFilterCondition))
		);
	}

	private static Connector mappingConnector(CQTableContext tableContext) {
		return tableContext.getConnectorTables().getConnector();
	}
}
