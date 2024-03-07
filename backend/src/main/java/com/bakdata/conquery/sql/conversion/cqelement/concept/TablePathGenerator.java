package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.ConceptConversionTables;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.Value;

@Value
class TablePathGenerator {

	SqlDialect sqlDialect;
	NameGenerator nameGenerator;

	public TablePathGenerator(ConversionContext context) {
		this.sqlDialect = context.getSqlDialect();
		this.nameGenerator = context.getNameGenerator();
	}

	public ConceptConversionTables createConnectorTables(CQConcept cqConcept, CQTable cqTable, String label) {
		TablePathInfo tableInfo = collectConnectorTables(cqConcept, cqTable);
		return create(tableInfo, label);
	}

	public ConceptConversionTables createUniversalTables(QueryStep predecessor, CQConcept cqConcept) {
		TablePathInfo tableInfo = collectConceptTables(predecessor, cqConcept);
		String conceptName = nameGenerator.conceptName(cqConcept);
		return create(tableInfo, conceptName);
	}

	private ConceptConversionTables create(TablePathInfo tableInfo, String label) {
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(tableInfo.getMappings().keySet(), label, nameGenerator);
		Preconditions.checkArgument(tableInfo.getLastPredecessor() != null, "TablePathInfo %s must contain a last predecessor CTE".formatted(tableInfo));
		String lastPredecessorName = cteNameMap.get(tableInfo.getLastPredecessor());
		return new ConceptConversionTables(
				tableInfo.getRootTable(),
				cteNameMap,
				tableInfo.getMappings(),
				lastPredecessorName,
				tableInfo.isContainsIntervalPacking()
		);
	}

	private TablePathInfo collectConnectorTables(CQConcept cqConcept, CQTable cqTable) {

		TablePathInfo tableInfo = new TablePathInfo();
		tableInfo.setRootTable(cqTable.getConnector().getTable().getName());
		tableInfo.addRequiredSteps(MANDATORY_STEPS);
		tableInfo.setLastPredecessor(JOIN_BRANCHES);

		// no validity date aggregation possible
		if (cqTable.findValidityDate() == null || cqConcept.isExcludeFromTimeAggregation()) {
			return tableInfo;
		}

		// interval packing required
		tableInfo.setContainsIntervalPacking(true);
		tableInfo.addMappings(IntervalPackingCteStep.getMappings(EVENT_FILTER, sqlDialect));

		// TODO handle event date selects

		return tableInfo;
	}

	private TablePathInfo collectConceptTables(QueryStep predecessor, CQConcept cqConcept) {

		TablePathInfo tableInfo = new TablePathInfo();
		tableInfo.setRootTable(predecessor.getCteName()); // last table of a single connector or merged and aggregated table of multiple connectors
		tableInfo.addRequiredStep(UNIVERSAL_SELECTS);

		// TODO handle event date selects

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
		 * When converting {@link Selects}, we need to qualify the final references onto the predecessor of the last CTE that is part of the conversion.
		 * It varies depending on the given {@link CQConcept}, thus we need to set it explicitly.
		 */
		private CteStep lastPredecessor;

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

		public void addRequiredSteps(Set<CteStep> steps) {
			this.mappings.putAll(CteStep.getDefaultPredecessorMap(steps));
		}

		public void addRequiredStep(CteStep step) {
			this.mappings.put(step, step.getPredecessor());
		}

	}

}
