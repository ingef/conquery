package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.EVENT_FILTER;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.MANDATORY_STEPS;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.UNIVERSAL_SELECTS;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep.INTERVAL_COMPLETE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.Getter;

/**
 * Determines all table/CTE names and creates the respective required {@link ConnectorSqlTables} and {@link ConceptSqlTables} which will be created during the
 * conversion of a {@link CQConcept}.
 */
class TablePath {

	private final Map<CQTable, ConnectorSqlTables> connectorTableMap = new HashMap<>();

	@Getter
	private final CQConcept cqConcept;

	@Getter
	private final ConversionContext context;

	public TablePath(CQConcept cqConcept, ConversionContext context) {
		this.cqConcept = cqConcept;
		this.context = context;
		cqConcept.getTables().forEach(cqTable -> this.connectorTableMap.put(cqTable, createConnectorTables(cqConcept, cqTable, context)));
	}

	public ConnectorSqlTables getConnectorTables(CQTable cqTable) {
		return connectorTableMap.get(cqTable);
	}

	private static ConnectorSqlTables createConnectorTables(CQConcept cqConcept, CQTable cqTable, ConversionContext context) {

		String conceptConnectorLabel = context.getNameGenerator().conceptConnectorName(cqConcept, cqTable.getConnector());
		TablePathInfo tableInfo = collectConnectorTables(cqConcept, cqTable, context);
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(tableInfo.getMappings().keySet(), conceptConnectorLabel, context.getNameGenerator());

		return new ConnectorSqlTables(
				conceptConnectorLabel,
				tableInfo.getRootTable(),
				cteNameMap,
				tableInfo.getMappings(),
				tableInfo.isContainsIntervalPacking()
		);
	}

	public ConceptSqlTables createConceptTables(QueryStep predecessor) {

		TablePathInfo tableInfo = collectConceptTables(predecessor);
		String conceptName = context.getNameGenerator().conceptName(cqConcept);
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(tableInfo.getMappings().keySet(), conceptName, context.getNameGenerator());
		List<ConnectorSqlTables> connectorSqlTables = this.connectorTableMap.values().stream().toList();

		return new ConceptSqlTables(
				conceptName,
				tableInfo.getRootTable(),
				cteNameMap,
				tableInfo.getMappings(),
				tableInfo.isContainsIntervalPacking(),
				connectorSqlTables
		);
	}

	private static TablePathInfo collectConnectorTables(CQConcept cqConcept, CQTable cqTable, ConversionContext context) {

		TablePathInfo tableInfo = new TablePathInfo();
		tableInfo.setRootTable(cqTable.getConnector().getTable().getName());
		tableInfo.addWithDefaultMapping(MANDATORY_STEPS);

		boolean eventDateSelectsPresent = cqTable.getSelects().stream().anyMatch(Select::isEventDateSelect);
		// no validity date aggregation possible nor necessary
		if (cqTable.findValidityDate() == null || (!cqConcept.isAggregateEventDates() && !eventDateSelectsPresent)) {
			return tableInfo;
		}

		// interval packing required
		tableInfo.setContainsIntervalPacking(true);
		tableInfo.addMappings(IntervalPackingCteStep.getMappings(EVENT_FILTER, context.getSqlDialect()));

		if (!eventDateSelectsPresent) {
			return tableInfo;
		}

		// interval packing selects required with optional unnest step
		if (context.getSqlDialect().supportsSingleColumnRanges()) {
			tableInfo.addMappings(Map.of(
					UNNEST_DATE, INTERVAL_COMPLETE,
					INTERVAL_PACKING_SELECTS, UNNEST_DATE
			));
		}
		else {
			tableInfo.addMappings(Map.of(
					INTERVAL_PACKING_SELECTS, INTERVAL_COMPLETE
			));
		}

		return tableInfo;
	}

	private TablePathInfo collectConceptTables(QueryStep predecessor) {

		TablePathInfo tableInfo = new TablePathInfo();
		tableInfo.setRootTable(predecessor.getCteName()); // last table of a single connector or merged and aggregated table of multiple connectors
		tableInfo.addRootTableMapping(UNIVERSAL_SELECTS);

		// no event date selects present
		if (cqConcept.getSelects().stream().noneMatch(Select::isEventDateSelect)) {
			return tableInfo;
		}

		Preconditions.checkArgument(
				predecessor.getSelects().getValidityDate().isPresent(),
				"Can not convert Selects that require interval packing without a validity date present after converting (a) connector(s)"
		);

		// universal event date selects required with optional additional unnest step
		if (context.getSqlDialect().supportsSingleColumnRanges()) {
			tableInfo.addRootTableMapping(UNNEST_DATE);
			tableInfo.addMappings(Map.of(INTERVAL_PACKING_SELECTS, UNNEST_DATE));
		}
		else {
			tableInfo.addRootTableMapping(INTERVAL_PACKING_SELECTS);
		}

		tableInfo.addMappings(Map.of(UNIVERSAL_SELECTS, INTERVAL_PACKING_SELECTS));

		return tableInfo;
	}

	@Data
	private static class TablePathInfo {

		/**
		 * Mapping of a CTE step to their respective preceding CTE step.
		 */
		private final Map<CteStep, CteStep> mappings;

		/**
		 * The root table is the predecessor of all CteSteps from {@link TablePathInfo#mappings} which have a null-predecessor.
		 */
		private String rootTable;

		/**
		 * True if this path info contains CTEs from {@link IntervalPackingCteStep}.
		 */
		private boolean containsIntervalPacking;

		public TablePathInfo() {
			this.mappings = new HashMap<>();
		}

		public void addMappings(Map<CteStep, CteStep> mappings) {
			this.mappings.putAll(mappings);
		}

		public void addWithDefaultMapping(Set<CteStep> steps) {
			this.mappings.putAll(CteStep.getDefaultPredecessorMap(steps));
		}

		public void addRootTableMapping(CteStep step) {
			this.mappings.put(step, null);
		}

	}

}
