package com.bakdata.conquery.sql.conversion.model.aggregator;

import static org.jooq.impl.DSL.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.FlagFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
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
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;
import org.jooq.Condition;
import org.jooq.Field;

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

	/**
	 * @return A mapping between a flags key and the corresponding {@link ExtractingSqlSelect} that will be created to reference the flag's column.
	 */
	private static Map<String, SingleColumnSqlSelect> createFlagRootSelectMap(FlagSelect flagSelect, String rootTable) {
		return flagSelect.getFlags()
						 .entrySet().stream()
						 .collect(Collectors.toMap(
								 Map.Entry::getKey,
								 entry -> {
									 Column column = entry.getValue().resolve();
									 Field<Object> field = field(name(rootTable, column.getName()));
									 return new FieldWrapper<>(field.as(column.getName()), column.getName()
									 );
								 }
						 ));
	}

	private static FieldWrapper<?> createFlagSelect(
			String alias,
			SqlTables connectorTables,
			SqlFunctionProvider functionProvider,
			Map<String, SingleColumnSqlSelect> flagRootSelectMap
	) {
		Map<String, Field<Boolean>> flagFieldsMap = createRootSelectReferences(connectorTables, flagRootSelectMap);

		// we first aggregate each flag column
		List<Field<String>> flagAggregations = new ArrayList<>();
		for (Map.Entry<String, Field<Boolean>> entry : flagFieldsMap.entrySet()) {
			Field<Boolean> boolColumn = entry.getValue();
			Condition anyTrue = functionProvider.orAgg(boolColumn);

			String flagName = entry.getKey();
			Field<String> flag = when(anyTrue, inline(flagName)).otherwise(""); // else null is implicit in SQL
			flagAggregations.add(flag);
		}

		// and stuff them into 1 array field
		Field<?> flagsArray = functionProvider.arrayOut(flagAggregations).as(alias);
		// we also need the references for all flag columns for the flag aggregation of multiple columns
		String[] requiredColumns = flagFieldsMap.values().stream().map(Field::getName).toArray(String[]::new);
		return new FieldWrapper<>(flagsArray, requiredColumns);
	}

	private static Map<String, Field<Boolean>> createRootSelectReferences(
			SqlTables connectorTables,
			Map<String, SingleColumnSqlSelect> flagRootSelectMap
	) {
		return flagRootSelectMap.entrySet().stream()
								.collect(Collectors.toMap(
										Map.Entry::getKey,
										entry -> (Field<Boolean>) entry.getValue().qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select()
								));
	}

	/**
	 * @return Columns names of a given flags map that match the selected flags of the filter value.
	 */
	private static List<Column> getRequiredColumns(Map<String, ColumnId> flags, Set<String> selectedFlags) {
		return selectedFlags.stream()
							.map(flags::get)
							.map(ColumnId::resolve)
							.toList();
	}

	@Override
	public ConnectorSqlSelects connectorSelect(FlagSelect flagSelect, SelectContext<ConnectorSqlTables> selectContext) {

		SqlFunctionProvider functionProvider = selectContext.getConversionContext().getDialectBundle().getFunctionProvider();
		SqlTables connectorTables = selectContext.getTables();

		Map<String, SingleColumnSqlSelect> rootSelects = createFlagRootSelectMap(flagSelect, connectorTables.getRootTable());

		String alias = selectContext.getNameGenerator().selectName(flagSelect);
		FieldWrapper<?> flagAggregation = createFlagSelect(alias, connectorTables, functionProvider, rootSelects);

		ExtractingSqlSelect<?> finalSelect = flagAggregation.qualify(connectorTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(rootSelects.values())
								  .aggregationSelect(flagAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(FlagFilter filter, FilterContext<Set<String>> filterContext) {

		List<Field<Boolean>> flagFields = getRequiredColumns(filter.getFlags(), filterContext.getValue())
				.stream()
				.map(column -> field(name(column.getTable().getName(), column.getName()), Boolean.class))
				.toList();

		FlagCondition flagCondition = new FlagCondition(flagFields);
		WhereClauses whereClauses = WhereClauses.builder()
												.eventFilter(flagCondition)
												.build();

		return new SqlFilters(ConnectorSqlSelects.none(), whereClauses);
	}

	@Override
	public Condition convertForTableExport(FlagFilter filter, FilterContext<Set<String>> filterContext) {

		List<Field<Boolean>> flagFields = getRequiredColumns(filter.getFlags(), filterContext.getValue())
				.stream()
				.map(column -> field(name(column.getTable().getName(), column.getName()), Boolean.class))
				.toList();

		return new FlagCondition(flagFields).condition();
	}

}
