package com.bakdata.conquery.sql.translate.query.concept;

import static com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep.PREPROCESSING;
import static com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep.UNIVERSAL_SELECTS;
import static com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.translate.query.interval.IntervalPackingCteStep.INTERVAL_COMPLETE;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.CteStep;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SchemaSql;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingCteStep;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import lombok.experimental.UtilityClass;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Plans the CTE graphs required to compile a resolved concept and its connectors. */
@UtilityClass
public class ConceptCtePlanner {

	/** Plan the CTE graph for one connector branch of a concept. */
	public ConnectorCtePlan planConnector(
			String rootTable,
			String connectorName,
			boolean aggregateEventDates,
			boolean connectorEventDateSelectsPresent,
			boolean conceptEventDateSelectsPresent,
			TranslatorDialect dialect,
			SqlNameGenerator nameGenerator
	) {
		return planConnector(
				DSL.table(DSL.name(rootTable)),
				rootTable,
				connectorName,
				aggregateEventDates,
				connectorEventDateSelectsPresent,
				conceptEventDateSelectsPresent,
				dialect,
				nameGenerator
		);
	}

	/** Plan one connector branch from a fully resolved physical source table. */
	public ConnectorCtePlan planConnector(
			SqlTable sourceTable,
			String connectorName,
			boolean aggregateEventDates,
			boolean connectorEventDateSelectsPresent,
			boolean conceptEventDateSelectsPresent,
			TranslatorDialect dialect,
			SqlNameGenerator nameGenerator
	) {
		String rootQualifier = sourceTable.physicalName().getLast();
		return planConnector(
				SchemaSql.table(sourceTable),
				rootQualifier,
				connectorName,
				aggregateEventDates,
				connectorEventDateSelectsPresent,
				conceptEventDateSelectsPresent,
				dialect,
				nameGenerator
		);
	}

	private ConnectorCtePlan planConnector(
			Table<Record> sourceTable,
			String rootQualifier,
			String connectorName,
			boolean aggregateEventDates,
			boolean connectorEventDateSelectsPresent,
			boolean conceptEventDateSelectsPresent,
			TranslatorDialect dialect,
			SqlNameGenerator nameGenerator
	) {
		Map<CteStep, CteStep> mappings = CteStep.getDefaultPredecessorMap(ConceptCteStep.MANDATORY_STEPS);
		boolean withIntervalPacking = aggregateEventDates
				|| connectorEventDateSelectsPresent
				|| conceptEventDateSelectsPresent;
		if (!withIntervalPacking) {
			return new ConnectorCtePlan(
					connectorName,
					sourceTable,
					createTables(rootQualifier, connectorName, mappings, nameGenerator),
					false
			);
		}

		mappings.putAll(IntervalPackingCteStep.getMappings(PREPROCESSING, dialect));
		if (connectorEventDateSelectsPresent) {
			addIntervalSelectMappings(mappings, INTERVAL_COMPLETE, dialect);
		}

		SqlTables tables = createTables(rootQualifier, connectorName, mappings, nameGenerator);
		return new ConnectorCtePlan(connectorName, sourceTable, tables, true);
	}

	/** Plan the final concept CTE graph over the converted connector branches. */
	public SqlTables planConcept(
			QueryStep predecessor,
			String conceptName,
			boolean eventDateSelectsPresent,
			TranslatorDialect dialect,
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

	private void addIntervalSelectMappings(Map<CteStep, CteStep> mappings, CteStep root, TranslatorDialect dialect) {
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
