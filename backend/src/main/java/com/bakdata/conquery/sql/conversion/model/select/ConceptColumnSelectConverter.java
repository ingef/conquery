package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.compiler.conversion.operation.SelectConversionContext;
import com.bakdata.conquery.sql.compiler.ir.SchemaSql;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSource;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/** Resolves concept connector columns and mapping-table sources before delegating SQL composition to the connector. */
public class ConceptColumnSelectConverter implements SelectConverter<ConceptColumnSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(ConceptColumnSelect select, SelectContext<ConnectorSqlTables> context) {
		Connector connector = context.getTables().getConnector();
		if (connector.getColumn() == null) {
			return ConnectorSqlSelects.none();
		}
		ResolvedColumn column = EntitySchemaAdapter.from(connector.getColumn().resolve());
		BuiltInSelects.ConceptValues operation = new BuiltInSelects.ConceptValues(select.getName(), List.of(column));
		return ResolvedSelectAdapter.connectorSelect(operation, select.getName(), context, preparedSources(select, List.of(connector), context));
	}

	@Override
	public ConceptSqlSelects conceptSelect(ConceptColumnSelect select, SelectContext<ConceptSqlTables> context) {
		List<? extends Connector> connectors = context.getTables().getConnectorTables().stream()
				.map(ConnectorSqlTables::getConnector)
				.toList();
		List<ResolvedColumn> columns = connectors.stream()
				.map(Connector::getColumn)
				.map(column -> EntitySchemaAdapter.from(column.resolve()))
				.toList();
		BuiltInSelects.ConceptValues operation = new BuiltInSelects.ConceptValues(select.getName(), columns);
		return ResolvedSelectAdapter.conceptSelect(operation, select.getName(), context, preparedSources(select, connectors, context));
	}

	private static List<SelectConversionContext.ConceptColumnSource> preparedSources(
			ConceptColumnSelect select,
			List<? extends Connector> connectors,
			SelectContext<?> context
	) {
		if (!select.isAsIds()) {
			return List.of();
		}
		return connectors.stream()
				.map(connector -> preparedSource(connector, (TreeConcept) select.getHolder().findConcept(), context))
				.toList();
	}

	private static SelectConversionContext.ConceptColumnSource preparedSource(
			Connector connector,
			TreeConcept concept,
			SelectContext<?> context
	) {
		ResolvedColumn column = EntitySchemaAdapter.from(connector.getColumn().resolve());
		Optional<ConnectorSqlTables> convertedConnector = context.getTables() instanceof ConceptSqlTables tables
				? tables.getConnectorTables().stream()
						.filter(connectorTables -> connectorTables.getConnector() != null)
						.filter(connectorTables -> connectorTables.getRootTable().equals(connector.resolveTableId().getTable()))
						.findFirst()
				: Optional.empty();

		if (convertedConnector.isPresent()) {
			String tableName = convertedConnector.orElseThrow().cteName(ConceptCteStep.PREPROCESSING);
			Table<Record> table = DSL.table(DSL.name(tableName));
			return new SelectConversionContext.ConceptColumnSource(
					tableName,
					table,
					DSL.field(DSL.name(tableName, column.physicalName())),
					List.of()
			);
		}

		ConceptIdMapping mapping = new ConceptIdMapping(concept, context.getFunctionProvider());
		Table<Record> connectorTable = SchemaSql.table(column.table());
		ConceptIdMappingSource mappingSource = mapping.source(connector);
		TableLike<? extends Record> sourceTable = mappingSource.resolvedConceptIds(
				connectorTable, context.getCompilerDialect()
		);
		Field<Integer> resolvedId = mappingSource.resolvedIdOrRoot(concept.getLocalId());
		Condition rawValuePresent = SchemaSql.field(column, Object.class).isNotNull();
		return new SelectConversionContext.ConceptColumnSource(
				connectorTable.getName(),
				sourceTable,
				resolvedId,
				List.of(rawValuePresent)
		);
	}
}
