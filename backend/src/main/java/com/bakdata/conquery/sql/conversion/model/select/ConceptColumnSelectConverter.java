package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptIdMapping;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class ConceptColumnSelectConverter implements SelectConverter<ConceptColumnSelect> {

	@Getter
	@RequiredArgsConstructor
	private enum CONCEPT_COLUMN_STEPS implements CteStep {

		UNIONED_COLUMNS("unioned_columns"),
		STRING_AGG("concept_column_aggregated");

		private final String suffix;
	}

	@Override
	public ConnectorSqlSelects connectorSelect(ConceptColumnSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		Connector connector = selectContext.getTables().getConnector();
		if (connector.getColumn() == null) {
			return ConnectorSqlSelects.none();
		}
		if (select.isAsIds()) {
			TreeConcept concept = (TreeConcept) select.getHolder().findConcept();
			ConceptIdMapping mapping = new ConceptIdMapping(concept, selectContext.getFunctionProvider());
			Field<Integer> resolvedId = DSL.coalesce(mapping.resolvedId(), DSL.inline(concept.getLocalId()))
					.as(connector.getColumn().getColumn());
			return ConnectorSqlSelects.builder()
					.preprocessingSelect(new FieldWrapper<>(resolvedId))
					.build();
		}
		ExtractingSqlSelect<Object> connectorColumn = new ExtractingSqlSelect<>(connector.resolveTableId().getTable(), connector.getColumn().getColumn(), Object.class);
		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(connectorColumn)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(ConceptColumnSelect select, SelectContext<ConceptSqlTables> selectContext) {

		// we will do a union distinct on all Connector tables
		List<? extends Connector> connectors;
		if (isSingleConnector(selectContext.getTables())) {
			// we union the Connector table with itself if there is only 1 Connector
			Connector connector = selectContext.getTables().getConnectorTables().get(0).getConnector();
			connectors = List.of(connector, connector);
		}
		else {
			connectors = selectContext.getTables().getConnectorTables().stream().map(ConnectorSqlTables::getConnector).toList();
		}

		NameGenerator nameGenerator = selectContext.getNameGenerator();
		String alias = nameGenerator.selectName(select);
		QueryStep unionStep = createUnionConnectorConnectorsStep(connectors, select, alias, selectContext);

		FieldWrapper<String> conceptColumnSelect = createConnectorColumnStringAgg(selectContext, unionStep, alias);
		Selects unionStepSelects = unionStep.getQualifiedSelects();
		Selects selects = Selects.builder()
								 .ids(unionStepSelects.getIds())
								 .sqlSelect(conceptColumnSelect)
								 .build();

		String stringAggCteName = nameGenerator.cteStepName(CONCEPT_COLUMN_STEPS.STRING_AGG, alias);
		QueryStep stringAggStep = QueryStep.builder()
										   .cteName(stringAggCteName)
										   .selects(selects)
										   .fromTable(QueryStep.toTableLike(unionStep.getCteName()))
										   .groupBy(unionStepSelects.getIds().toFields())
										   .predecessor(unionStep)
										   .build();

		ExtractingSqlSelect<String> finalSelect = conceptColumnSelect.qualify(stringAggStep.getCteName());

		return ConceptSqlSelects.builder()
								.additionalPredecessor(Optional.of(stringAggStep))
								.finalSelect(finalSelect)
								.build();
	}

	private static boolean isSingleConnector(ConceptSqlTables tables) {
		return tables.getConnectorTables().size() == 1;
	}

	private static QueryStep createUnionConnectorConnectorsStep(
			List<? extends Connector> connectors,
			ConceptColumnSelect select,
			String alias,
			SelectContext<ConceptSqlTables> selectContext
	) {
		List<QueryStep> unionSteps = connectors.stream().map(connector -> createConnectorColumnSelectQuery(connector, select, alias, selectContext)).toList();
		String unionedColumnsCteName = selectContext.getNameGenerator().cteStepName(CONCEPT_COLUMN_STEPS.UNIONED_COLUMNS, alias);
		return QueryStep.createUnionStep(unionSteps, unionedColumnsCteName, Collections.emptyList(), false, selectContext.getFunctionProvider()); //TODO is false correct here?
	}

	private static QueryStep createConnectorColumnSelectQuery(
			Connector connector,
			ConceptColumnSelect select,
			String alias,
			SelectContext<ConceptSqlTables> selectContext
	) {
		// a  ConceptColumn select uses all connectors a Concept has, even if they are not part of the CQConcept
		// but if they are, we need to make sure we use the preprocessed and event-filtered table instead of the root table
		Optional<ConnectorSqlTables> convertedConnector = selectContext.getTables()
										.getConnectorTables()
										.stream()
										.filter(tables -> Objects.equals(tables.getRootTable(), connector.resolveTableId().getTable()))
										.findFirst();
		String tableName = convertedConnector
				.map(tables -> tables.cteName(ConceptCteStep.PREPROCESSING))
				.orElse(connector.resolveTableId().getTable());

		Table<Record> connectorTable = DSL.table(DSL.name(tableName));
		SqlIdColumns ids = selectContext.getIds().qualify(connectorTable.getName());
		Field<?> connectorColumn = DSL.field(DSL.name(connectorTable.getName(), connector.getColumn().resolve().getName()));
		TableLike<? extends Record> sourceTable = connectorTable;
		List<org.jooq.Condition> conditions = Collections.emptyList();

		if (select.isAsIds() && convertedConnector.isEmpty()) {
			TreeConcept concept = (TreeConcept) select.getHolder().findConcept();
			ConceptIdMapping mapping = new ConceptIdMapping(concept, selectContext.getFunctionProvider());
			sourceTable = selectContext.getFunctionProvider().leftJoin(
					connectorTable,
					mapping.table(),
					List.of(mapping.joinCondition(connector))
			);
			connectorColumn = DSL.coalesce(mapping.resolvedId(), DSL.inline(concept.getLocalId()));
			Field<Object> rawConceptColumn = DSL.field(DSL.name(connectorTable.getName(), connector.getColumn().resolve().getName()));
			conditions = List.of(rawConceptColumn.isNotNull());
		}

		Field<String> casted = selectContext.getFunctionProvider().cast(connectorColumn, SQLDataType.VARCHAR).as(alias);
		FieldWrapper<String> connectorSelect = new FieldWrapper<>(casted);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(connectorSelect)
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(sourceTable)
						.conditions(conditions)
						.build();
	}

	private static FieldWrapper<String> createConnectorColumnStringAgg(SelectContext<ConceptSqlTables> selectContext, QueryStep unionStep, String alias) {
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		Field<String> unionedColumn = DSL.field(DSL.name(unionStep.getCteName(), alias), String.class);
		return new FieldWrapper<>(
				functionProvider.stringAggregation(unionedColumn, DSL.toChar(ResultSetProcessor.UNIT_SEPARATOR), List.of(unionedColumn)).as(alias)
		);
	}

}
