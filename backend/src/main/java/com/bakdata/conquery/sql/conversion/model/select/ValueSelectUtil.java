package com.bakdata.conquery.sql.conversion.model.select;

import static org.jooq.impl.DSL.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import org.jetbrains.annotations.NotNull;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.SortField;

class ValueSelectUtil {

	public static ConnectorSqlSelects createValueSelect(
			Column column,
			String alias,
			Function<Field<?>, ? extends SortField<?>> ordering,
			Range.IntegerRange substringRange, SelectContext<ConnectorSqlTables> selectContext) {


		SingleColumnSqlSelect rootSelect = MappableSingleColumnSelect.getSubstringSelect(column, substringRange, selectContext, alias);
		FieldWrapper<Integer> rowNumberSelect = rowNumberField(rootSelect, ordering, alias, selectContext);

		ConnectorSqlTables tables = selectContext.getTables();
		String preprocessingCte = tables.cteName(ConceptCteStep.PREPROCESSING);
		Field<?> qualifiedRootSelect = rootSelect.qualify(preprocessingCte).select();
		Field<Integer> qualifiedRowNumber = rowNumberSelect.qualify(preprocessingCte).select();
		Field<?> rankedValue = when(qualifiedRowNumber.equal(inline(1)), qualifiedRootSelect);
		Field<?> anyValue = selectContext.getFunctionProvider().anyValue(rankedValue).as(alias);
		FieldWrapper<?> aggregationSelect = new FieldWrapper<>(anyValue, column.getName());

		ExtractingSqlSelect<?> finalSelect = aggregationSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(rootSelect)
								  .preprocessingSelect(rowNumberSelect)
								  .aggregationSelect(aggregationSelect)
								  .finalSelect(finalSelect)
								  .build();
	}

	@NotNull
	private static FieldWrapper<Integer> rowNumberField(
			SingleColumnSqlSelect rootSelect,
			Function<Field<?>, ? extends SortField<?>> ordering,
			String alias,
			SelectContext<ConnectorSqlTables> selectContext
	) {
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		List<Field<?>> ids = selectContext.getIds().toFields();
		List<OrderField<?>> orderByFields = new ArrayList<>();

		// Previously null values were filtered before row numbering. Keep that behavior while calculating the window in preprocessing.
		orderByFields.add(when(rootSelect.select().isNull(), inline(1)).otherwise(inline(0)).asc());
		Optional<ColumnDateRange> validityDate = selectContext.getValidityDate();
		if (validityDate.isPresent()) {
			orderByFields.addAll(functionProvider.orderByValidityDates(ordering, validityDate.get().toFields()));
		}
		else {
			orderByFields.addAll(ids);
		}

		return new FieldWrapper<>(
				rowNumber().over(partitionBy(ids).orderBy(orderByFields)).as("%s-row-number".formatted(alias)),
				new String[0]
		);
	}

}
