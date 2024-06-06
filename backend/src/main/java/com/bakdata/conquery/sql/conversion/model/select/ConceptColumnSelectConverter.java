package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
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
	public ConnectorSqlSelects connectorSelect(ConceptColumnSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {
		Connector connector = selectContext.getSelectHolder();
		if (connector.getColumn() == null) {
			return ConnectorSqlSelects.none();
		}
		ExtractingSqlSelect<Object> connectorColumn = new ExtractingSqlSelect<>(connector.getTable().getName(), connector.getColumn().getName(), Object.class);
		ExtractingSqlSelect<Object> qualified = connectorColumn.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.EVENT_FILTER));
		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(connectorColumn)
								  .connectorColumn(Optional.of(qualified))
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(ConceptColumnSelect select, SelectContext<TreeConcept, ConceptSqlTables> selectContext) {

		// we will do a union distinct on all Connector tables
		List<? extends Connector> connectors;
		if (isSingleConnectorConcept(selectContext.getSelectHolder())) {
			// we union the Connector table with itself if there is only 1 Connector
			Connector connector = selectContext.getSelectHolder().getConcept().getConnectors().get(0);
			connectors = List.of(connector, connector);
		}
		else {
			connectors = selectContext.getSelectHolder().getConnectors();
		}

		NameGenerator nameGenerator = selectContext.getNameGenerator();
		String alias = nameGenerator.selectName(select);
		QueryStep unionStep = createUnionConnectorConnectorsStep(connectors, alias, selectContext);

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

	private static boolean isSingleConnectorConcept(TreeConcept treeConcept) {
		return treeConcept.getConcept().getConnectors().size() == 1;
	}

	private static QueryStep createUnionConnectorConnectorsStep(
			List<? extends Connector> connectors,
			String alias,
			SelectContext<TreeConcept, ConceptSqlTables> selectContext
	) {
		List<QueryStep> unionSteps = connectors.stream()
											   .filter(connector -> connector.getColumn() != null)
											   .map(connector -> createConnectorColumnSelectQuery(connector, alias, selectContext))
											   .toList();

		String unionedColumnsCteName = selectContext.getNameGenerator().cteStepName(CONCEPT_COLUMN_STEPS.UNIONED_COLUMNS, alias);
		return QueryStep.createUnionStep(unionSteps, unionedColumnsCteName, Collections.emptyList());
	}

	private static QueryStep createConnectorColumnSelectQuery(
			Connector connector,
			String alias,
			SelectContext<TreeConcept, ConceptSqlTables> selectContext
	) {
		// a  ConceptColumn select uses all connectors a Concept has, even if they are not part of the CQConcept
		// but if they are, we need to make sure we use the event-filtered table instead of the root table
		String tableName = selectContext.getTables()
										.getConnectorTables()
										.stream()
										.filter(tables -> Objects.equals(tables.getRootTable(), connector.getTable().getName()))
										.findFirst()
										.map(tables -> tables.cteName(ConceptCteStep.EVENT_FILTER))
										.orElse(connector.getTable().getName());

		Table<Record> connectorTable = DSL.table(DSL.name(tableName));

		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(connector.getTable(), selectContext.getConversionContext().getConfig());
		Field<Object> qualifiedPrimaryColumn = QualifyingUtil.qualify(primaryColumn, connectorTable.getName()).as(SharedAliases.PRIMARY_COLUMN.getAlias());
		SqlIdColumns ids = new SqlIdColumns(qualifiedPrimaryColumn);

		Field<Object> connectorColumn = DSL.field(DSL.name(connectorTable.getName(), connector.getColumn().getName()));
		Field<String> casted = selectContext.getFunctionProvider().cast(connectorColumn, SQLDataType.VARCHAR).as(alias);
		FieldWrapper<String> connectorSelect = new FieldWrapper<>(casted);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(connectorSelect)
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(connectorTable)
						.build();
	}

	private static FieldWrapper<String> createConnectorColumnStringAgg(SelectContext<TreeConcept, ConceptSqlTables> selectContext, QueryStep unionStep, String alias) {
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		Field<String> unionedColumn = DSL.field(DSL.name(unionStep.getCteName(), alias), String.class);
		return new FieldWrapper<>(
				functionProvider.stringAggregation(unionedColumn, DSL.toChar(ResultSetProcessor.UNIT_SEPARATOR), List.of(unionedColumn)).as(alias)
		);
	}
}
