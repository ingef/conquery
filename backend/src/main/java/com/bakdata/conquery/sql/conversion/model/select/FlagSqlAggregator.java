package com.bakdata.conquery.sql.conversion.model.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.FlagFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.FlagSelect;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.FlagCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * {@link FlagSelect} conversion aggregates the keys of the flags of a {@link FlagSelect} into a list.
 * <p>
 * If any value of the respective flag column is true, the flag key will be part of the string aggregation. <br>
 * If no value is true, an empty string will be added as value, because a {@code null} value would cause the whole string aggregation to be {@code null} too. <br>
 * Each value will be followed by the {@link ResultSetProcessor#UNIT_SEPARATOR}.
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
 *		select "pid"
 *		from "preprocessing"
 *		where (
 *			"preprocessing"."b" = true
 *			or "preprocessing"."c" = true
 *		)
 * )
 * }
 * </pre>
 */
@Value
public class FlagSqlAggregator implements SqlAggregator {

	private static final Param<Integer> NUMERIC_TRUE_VAL = DSL.val(1);
	private static final Param<Integer> NUMERIC_FALSE_VAL = DSL.val(0);
	private static final Param<String> EMPTY_STRING = DSL.val("");

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public static FlagSqlAggregator create(FlagSelect flagSelect, SelectContext selectContext) {

		SqlFunctionProvider functionProvider = selectContext.getParentContext().getSqlDialect().getFunctionProvider();
		SqlTables<ConceptCteStep> conceptTables = selectContext.getConceptTables();

		String rootTable = conceptTables.getPredecessor(ConceptCteStep.PREPROCESSING);
		Map<String, SqlSelect> rootSelects = createFlagRootSelectMap(flagSelect, rootTable);

		String alias = selectContext.getNameGenerator().selectName(flagSelect);
		FieldWrapper<Object[]> flagAggregation = createFlagSelect(alias, conceptTables, functionProvider, rootSelects);

		ExtractingSqlSelect<Object[]> finalSelect = flagAggregation.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.FINAL));

		SqlSelects sqlSelects = SqlSelects.builder().preprocessingSelects(rootSelects.values())
										  .aggregationSelect(flagAggregation)
										  .finalSelect(finalSelect)
										  .build();

		return new FlagSqlAggregator(sqlSelects, WhereClauses.builder().build());
	}

	public static FlagSqlAggregator create(FlagFilter flagFilter, FilterContext<String[]> filterContext) {
		SqlTables<ConceptCteStep> conceptTables = filterContext.getConceptTables();
		String rootTable = conceptTables.getPredecessor(ConceptCteStep.PREPROCESSING);

		List<SqlSelect> rootSelects =
				getRequiredColumnNames(flagFilter.getFlags(), filterContext.getValue())
						.stream()
						.map(columnName -> new ExtractingSqlSelect<>(rootTable, columnName, Boolean.class))
						.collect(Collectors.toList());
		SqlSelects selects = SqlSelects.builder()
									   .preprocessingSelects(rootSelects)
									   .build();

		List<Field<Boolean>> flagFields = rootSelects.stream()
													 .map(sqlSelect -> conceptTables.<Boolean>qualifyOnPredecessor(ConceptCteStep.EVENT_FILTER, sqlSelect.aliased()))
													 .toList();
		FlagCondition flagCondition = new FlagCondition(flagFields);
		WhereClauses whereClauses = WhereClauses.builder()
												.eventFilter(flagCondition)
												.build();

		return new FlagSqlAggregator(selects, whereClauses);
	}

	/**
	 * @return A mapping between a flags key and the corresponding {@link ExtractingSqlSelect} that will be created to reference the flag's column.
	 */
	private static Map<String, SqlSelect> createFlagRootSelectMap(FlagSelect flagSelect, String rootTable) {
		return flagSelect.getFlags()
						 .entrySet().stream()
						 .collect(Collectors.toMap(
								 Map.Entry::getKey,
								 entry -> new ExtractingSqlSelect<>(rootTable, entry.getValue().getName(), Boolean.class)
						 ));
	}

	private static FieldWrapper<Object[]> createFlagSelect(
			String alias,
			SqlTables<ConceptCteStep> conceptTables,
			SqlFunctionProvider functionProvider,
			Map<String, SqlSelect> flagRootSelectMap
	) {
		Map<String, Field<Boolean>> flagFieldsMap = createRootSelectReferences(conceptTables, flagRootSelectMap);

		// we first aggregate each flag column
		List<Field<?>> flagAggregations = new ArrayList<>();
		for (Map.Entry<String, Field<Boolean>> entry : flagFieldsMap.entrySet()) {
			Field<Boolean> boolColumn = entry.getValue();
			Condition anyTrue = DSL.max(functionProvider.cast(boolColumn, SQLDataType.INTEGER))
								   .eq(NUMERIC_TRUE_VAL);
			String flagName = entry.getKey();
			Field<String> flag = DSL.when(anyTrue, DSL.val(flagName)); // else null is implicit in SQL
			flagAggregations.add(flag);
		}

		// and stuff them into 1 array field
		Field<Object[]> flagsArray = functionProvider.asArray(flagAggregations).as(alias);
		return new FieldWrapper<>(flagsArray);
	}

	private static Map<String, Field<Boolean>> createRootSelectReferences(SqlTables<ConceptCteStep> conceptTables, Map<String, SqlSelect> flagRootSelectMap) {
		return flagRootSelectMap.entrySet().stream()
						  .collect(Collectors.toMap(
								  Map.Entry::getKey,
								  entry -> conceptTables.qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, entry.getValue().aliased())
						  ));
	}

	/**
	 * @return Columns names of a given flags map that match the selected flags of the filter value.
	 */
	private static List<String> getRequiredColumnNames(Map<String, Column> flags, String[] selectedFlags) {
		return Arrays.stream(selectedFlags)
					 .map(flags::get)
					 .map(NamedImpl::getName)
					 .toList();
	}

}
