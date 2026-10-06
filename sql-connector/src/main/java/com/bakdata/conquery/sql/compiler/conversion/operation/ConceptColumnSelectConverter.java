package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class ConceptColumnSelectConverter {

	@Getter
	@RequiredArgsConstructor
	private enum CONCEPT_COLUMN_STEPS implements CteStep {

		UNIONED_COLUMNS("unioned_columns"),
		STRING_AGG("concept_column_aggregated");

		private final String suffix;
	}


	public ConnectorSqlSelects connectorSelect(BuiltInSelects.ConceptValues select, SelectConversionContext context) {
		return ConnectorSqlSelects.builder().preprocessingSelects(IntStream.range(0, select.columns().size())
				.mapToObj(index -> {
					ResolvedColumn column = select.columns().get(index);
					SelectConversionContext.ConceptColumnSource source = preparedSource(context, index);
					if (source != null) {
						return new FieldWrapper<>(source.value().as(column.physicalName()));
					}
					return new ExtractingSqlSelect<>(column.table().physicalName().getLast(), column.physicalName(), Object.class);
				})
				.toList()).build();
	}

	public ConceptSqlSelects conceptSelect(BuiltInSelects.ConceptValues select, SelectConversionContext selectContext) {

		// we will do a union distinct on all Connector tables
		List<Integer> connectorIndexes;
		if (select.columns().size() == 1) {
			// Preserve the self-union for a single connector.
			connectorIndexes = List.of(0, 0);
		}
		else {
			connectorIndexes = IntStream.range(0, select.columns().size()).boxed().toList();
		}
		SqlNameGenerator nameGenerator = selectContext.nameGenerator();
		String alias = selectContext.alias();
		QueryStep unionStep = createUnionConnectorConnectorsStep(select.columns(), connectorIndexes, alias, selectContext);

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

	private static QueryStep createUnionConnectorConnectorsStep(
			List<ResolvedColumn> connectors,
			List<Integer> connectorIndexes,
			String alias,
			SelectConversionContext selectContext
	) {
		List<QueryStep> unionSteps = connectorIndexes.stream()
				.map(index -> createConnectorColumnSelectQuery(connectors.get(index), preparedSource(selectContext, index), alias, selectContext))
				.toList();
		String unionedColumnsCteName = selectContext.nameGenerator().cteStepName(CONCEPT_COLUMN_STEPS.UNIONED_COLUMNS, alias);
		return QueryStep.createUnionStep(unionSteps, unionedColumnsCteName, Collections.emptyList(), false); //TODO is false correct here?
	}

	private static QueryStep createConnectorColumnSelectQuery(
			ResolvedColumn connector,
			SelectConversionContext.ConceptColumnSource preparedSource,
			String alias,
			SelectConversionContext selectContext
	) {
		// a  ConceptColumn select uses all connectors a Concept has, even if they are not part of the CQConcept
		// but if they are, we need to make sure we use the preprocessed and event-filtered table instead of the root table
		String physicalTable = connector.table().physicalName().getLast();
		String tableName = preparedSource != null
				? preparedSource.qualifier()
				: selectContext.conceptColumnTables().getOrDefault(physicalTable, physicalTable);
		Table<Record> connectorTable = DSL.table(DSL.name(tableName));
		SqlIdColumns ids = selectContext.ids().qualify(connectorTable.getName());
		Field<?> connectorColumn = preparedSource != null
				? preparedSource.value()
				: DSL.field(DSL.name(connectorTable.getName(), connector.physicalName()));
		Field<String> casted = selectContext.dialect().cast(connectorColumn, SQLDataType.VARCHAR).as(alias);
		FieldWrapper<String> connectorSelect = new FieldWrapper<>(casted);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(connectorSelect)
								 .build();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(preparedSource != null ? preparedSource.table() : connectorTable)
						.conditions(preparedSource != null ? preparedSource.conditions() : List.of())
						.build();
	}

	private static SelectConversionContext.ConceptColumnSource preparedSource(SelectConversionContext context, int index) {
		return context.conceptColumnSources().isEmpty() ? null : context.conceptColumnSources().get(index);
	}

	private static FieldWrapper<String> createConnectorColumnStringAgg(SelectConversionContext selectContext, QueryStep unionStep, String alias) {
		CompilerDialect functionProvider = selectContext.dialect();
		Field<String> unionedColumn = DSL.field(DSL.name(unionStep.getCteName(), alias), String.class);
		return new FieldWrapper<>(
				functionProvider.stringAggregation(unionedColumn, DSL.toChar((char) 31), List.of(unionedColumn)).as(alias)
		);
	}

}
