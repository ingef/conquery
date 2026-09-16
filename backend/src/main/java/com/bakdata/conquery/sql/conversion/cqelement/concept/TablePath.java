package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCtePlanner;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorCtePlan;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import lombok.Getter;

/**
 * Resolves legacy concept models and attaches their backend metadata to the connector-owned CTE plans.
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
		Connector connector = cqTable.getConnector().resolve();
		String connectorName = context.getNameGenerator().legacyConceptConnectorName(
				cqConcept.userLabel(context.getSqlPrintSettings().getLocale()),
				connector.getName()
		);
		boolean eventDateSelectsPresent = cqTable.getSelects().stream()
				.map(SelectId::resolve)
				.anyMatch(Select::isEventDateSelect);
		ConnectorCtePlan plan = ConceptCtePlanner.planConnector(
				connector.resolveTableId().getTable(),
				connectorName,
				cqConcept.isAggregateEventDates(),
				eventDateSelectsPresent,
				context.getCompilerDialect(),
				context.getNameGenerator()
		);

		return new ConnectorSqlTables(connector, connectorName, plan);
	}

	public ConceptSqlTables createConceptTables(QueryStep predecessor) {
		String conceptName = context.getNameGenerator().legacyConceptName(cqConcept.userLabel(context.getSqlPrintSettings().getLocale()));
		boolean eventDateSelectsPresent = cqConcept.getSelects().stream()
				.map(SelectId::resolve)
				.anyMatch(Select::isEventDateSelect);
		SqlTables tables = ConceptCtePlanner.planConcept(
				predecessor,
				conceptName,
				eventDateSelectsPresent,
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
		List<ConnectorSqlTables> connectorSqlTables = this.connectorTableMap.values().stream().toList();

		return new ConceptSqlTables(tables, connectorSqlTables);
	}
}
