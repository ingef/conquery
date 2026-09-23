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
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.compiler.ir.condition.FlagCondition;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
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
 * "preprocessing" as (
 * 		select "pid"
 * 		from "root_table"
 * 		where (
 * 			"root_table"."b" = true
 * 			or "root_table"."c" = true
 * 		)
 * )
 * }
 * </pre>
 */
public class FlagSqlAggregator implements SelectConverter<FlagSelect>, FilterConverter<FlagFilter, Set<String>>, SqlAggregator {

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
		var flags = flagSelect.getFlags().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
				entry -> com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter.from(entry.getValue().resolve())));
		return com.bakdata.conquery.sql.conversion.model.select.ResolvedSelectAdapter.connectorSelect(
				new com.bakdata.conquery.sql.model.operation.BuiltInSelects.Aggregation(flagSelect.getName(),
						new com.bakdata.conquery.sql.model.operation.BuiltInAggregations.Flags(flags)), flagSelect.getName(), selectContext);
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

		List<Field<Boolean>> flagFields = rootSelects.stream()
													 .map(ExtractingSqlSelect::select)
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
