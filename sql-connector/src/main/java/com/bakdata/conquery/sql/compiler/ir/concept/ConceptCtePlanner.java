package com.bakdata.conquery.sql.compiler.ir.concept;

import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.PREPROCESSING;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.UNIVERSAL_SELECTS;
import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep.INTERVAL_COMPLETE;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingCteStep;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import lombok.experimental.UtilityClass;

/** Plans the CTE graphs required to compile a resolved concept and its connectors. */
@UtilityClass
public class ConceptCtePlanner {

	/** Plan the CTE graph for one connector branch of a concept. */
	public ConnectorCtePlan planConnector(
			String rootTable,
			String connectorName,
			boolean aggregateEventDates,
			boolean eventDateSelectsPresent,
			CompilerDialect dialect,
			SqlNameGenerator nameGenerator
	) {
		Map<CteStep, CteStep> mappings = CteStep.getDefaultPredecessorMap(ConceptCteStep.MANDATORY_STEPS);
		boolean withIntervalPacking = aggregateEventDates || eventDateSelectsPresent;
		if (!withIntervalPacking) {
			return new ConnectorCtePlan(connectorName, createTables(rootTable, connectorName, mappings, nameGenerator), false, false);
		}

		mappings.putAll(IntervalPackingCteStep.getMappings(PREPROCESSING, dialect));
		if (eventDateSelectsPresent) {
			addIntervalSelectMappings(mappings, INTERVAL_COMPLETE, dialect);
		}

		SqlTables tables = createTables(rootTable, connectorName, mappings, nameGenerator);
		return new ConnectorCtePlan(connectorName, tables, true, !aggregateEventDates);
	}

	/** Plan the final concept CTE graph over the converted connector branches. */
	public SqlTables planConcept(
			QueryStep predecessor,
			String conceptName,
			boolean eventDateSelectsPresent,
			CompilerDialect dialect,
			SqlNameGenerator nameGenerator
	) {
		Map<CteStep, CteStep> mappings = new HashMap<>();
		mappings.put(UNIVERSAL_SELECTS, null);
		if (!eventDateSelectsPresent) {
			return createTables(predecessor.getCteName(), conceptName, mappings, nameGenerator);
		}

		if (predecessor.getSelects().getValidityDate().isEmpty()) {
			throw new IllegalArgumentException(
					"Can not convert Selects that require interval packing without a validity date present after converting (a) connector(s)"
			);
		}

		addIntervalSelectMappings(mappings, null, dialect);
		mappings.put(UNIVERSAL_SELECTS, INTERVAL_PACKING_SELECTS);
		return createTables(predecessor.getCteName(), conceptName, mappings, nameGenerator);
	}

	private void addIntervalSelectMappings(Map<CteStep, CteStep> mappings, CteStep root, CompilerDialect dialect) {
		if (dialect.supportsSingleColumnRanges()) {
			mappings.put(UNNEST_DATE, root);
			mappings.put(INTERVAL_PACKING_SELECTS, UNNEST_DATE);
		}
		else {
			mappings.put(INTERVAL_PACKING_SELECTS, root);
		}
	}

	private SqlTables createTables(
			String rootTable,
			String label,
			Map<CteStep, CteStep> mappings,
			SqlNameGenerator nameGenerator
	) {
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(
				mappings.keySet(),
				label,
				nameGenerator::cteStepName
		);
		return new SqlTables(rootTable, cteNameMap, mappings);
	}
}
