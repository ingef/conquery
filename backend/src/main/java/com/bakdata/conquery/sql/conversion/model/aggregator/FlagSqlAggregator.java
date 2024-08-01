package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.FlagFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.FlagCondition;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * {@link FlagSelect} conversion aggregates the keys of the flags of a {@link FlagSelect} into an array.
 * <p>
 * If any value of the respective flag column is true, the flag key will be part of the generated array. <br>
 *
 * <pre>
 * {@code
 * "group_select" as (
 * 		select
 * 			"pid",
 * 			array[
 * 				case when max(cast("concept_flags-1-preprocessing"."a" as integer)) = 1 then 'A' end,
 * 				case when max(cast("concept_flags-1-preprocessing"."b" as integer)) = 1 then 'B' end,
 * 				case when max(cast("concept_flags-1-preprocessing"."c" as integer)) = 1 then 'C' end
 * 				] as "flags_selects-1"
 * 		from "preprocessing"
 * 		group by "pid"
 * )
 * }
 * </pre>
 *
 * <hr>
 * <p>
 * {@link FlagFilter} conversion filters events if not at least 1 of the flag columns has a true value for the corresponding entry.
 *
 * <pre>
 * {@code
 * "event_filter" as (
 * 		select "pid"
 * 		from "preprocessing"
 * 		where (
 * 			"preprocessing"."b" = true
 * 			or "preprocessing"."c" = true
 * 		)
 * )
 * }
 * </pre>
 */
public class FlagSqlAggregator implements SelectConverter<FlagSelect>, FilterConverter<FlagFilter, Set<String>>, SqlAggregator {

	private static final Param<Integer> NUMERIC_TRUE_VAL = DSL.val(1);

	@Override
	public ConnectorSqlSelects connectorSelect(FlagSelect flagSelect, SelectContext<ConnectorSqlTables> selectContext) {

		SqlFunctionProvider functionProvider = selectContext.getConversionContext().getSqlDialect().getFunctionProvider();
		SqlTables connectorTables = selectContext.getTables();

		Map<String, ExtractingSqlSelect<Boolean>> rootSelects = createFlagRootSelectMap(flagSelect, connectorTables.getRootTable());

		String alias = selectContext.getNameGenerator().selectName(flagSelect);
		FieldWrapper<String> flagAggregation = createFlagSelect(alias, connectorTables, functionProvider, rootSelects);

		ExtractingSqlSelect<String> finalSelect = flagAggregation.qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(rootSelects.values())
								  .aggregationSelect(flagAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}

	/**
	 * @return A mapping between a flags key and the corresponding {@link ExtractingSqlSelect} that will be created to reference the flag's column.
	 */
	private static Map<String, ExtractingSqlSelect<Boolean>> createFlagRootSelectMap(FlagSelect flagSelect, String rootTable) {
		return flagSelect.getFlags()
						 .entrySet().stream()
						 .collect(Collectors.toMap(
								 Map.Entry::getKey,
								 entry -> new ExtractingSqlSelect<>(rootTable, entry.getValue().getName(), Boolean.class)
						 ));
	}

	private static FieldWrapper<String> createFlagSelect(
			String alias,
			SqlTables connectorTables,
			SqlFunctionProvider functionProvider,
			Map<String, ExtractingSqlSelect<Boolean>> flagRootSelectMap
	) {
		Map<String, Field<Boolean>> flagFieldsMap = createRootSelectReferences(connectorTables, flagRootSelectMap);

		// we first aggregate each flag column
		List<Field<String>> flagAggregations = new ArrayList<>();
		for (Map.Entry<String, Field<Boolean>> entry : flagFieldsMap.entrySet()) {
			Field<Boolean> boolColumn = entry.getValue();
			Condition anyTrue = DSL.max(functionProvider.cast(boolColumn, SQLDataType.INTEGER))
								   .eq(NUMERIC_TRUE_VAL);

			String flagName = entry.getKey();
			Field<String> flag = DSL.when(anyTrue, DSL.val(flagName)); // else null is implicit in SQL
			flagAggregations.add(flag);
		}

		// and stuff them into 1 array field
		Field<String> flagsArray = functionProvider.concat(flagAggregations).as(alias);
		// we also need the references for all flag columns for the flag aggregation of multiple columns
		String[] requiredColumns = flagFieldsMap.values().stream().map(Field::getName).toArray(String[]::new);
		return new FieldWrapper<>(flagsArray, requiredColumns);
	}

	private static Map<String, Field<Boolean>> createRootSelectReferences(
			SqlTables connectorTables,
			Map<String, ExtractingSqlSelect<Boolean>> flagRootSelectMap
	) {
		return flagRootSelectMap.entrySet().stream()
								.collect(Collectors.toMap(
										Map.Entry::getKey,
										entry -> entry.getValue().qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select()
								));
	}

	@Override
	public SqlFilters convertToSqlFilter(FlagFilter flagFilter, FilterContext<Set<String>> filterContext) {
		SqlTables connectorTables = filterContext.getTables();
		String rootTable = connectorTables.getPredecessor(ConceptCteStep.PREPROCESSING);

		List<ExtractingSqlSelect<Boolean>> rootSelects = getRequiredColumns(flagFilter.getFlags(), filterContext.getValue())
				.stream()
				.map(Column::getName)
				.map(columnName -> new ExtractingSqlSelect<>(rootTable, columnName, Boolean.class))
				.collect(Collectors.toList());

		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(rootSelects)
														 .build();

		List<Field<Boolean>> flagFields = rootSelects.stream()
													 .map(sqlSelect -> sqlSelect.qualify(connectorTables.getPredecessor(ConceptCteStep.EVENT_FILTER)).select())
													 .toList();
		FlagCondition flagCondition = new FlagCondition(flagFields);
		WhereClauses whereClauses = WhereClauses.builder()
												.eventFilter(flagCondition)
												.build();

		return new SqlFilters(selects, whereClauses);
	}

	/**
	 * @return Columns names of a given flags map that match the selected flags of the filter value.
	 */
	private static List<Column> getRequiredColumns(Map<String, Column> flags, Set<String> selectedFlags) {
		return selectedFlags.stream()
							.map(flags::get)
							.toList();
	}

	@Override
	public Condition convertForTableExport(FlagFilter filter, FilterContext<Set<String>> filterContext) {

		List<Field<Boolean>> flagFields = getRequiredColumns(filter.getFlags(), filterContext.getValue())
				.stream()
				.map(column -> DSL.field(DSL.name(column.getTable().getName(), column.getName()), Boolean.class))
				.toList();

		return new FlagCondition(flagFields).condition();
	}

}
