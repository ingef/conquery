package com.bakdata.conquery.sql.compiler.conversion.aggregation;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.compiler.query.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import org.jooq.Field;
import org.jooq.impl.DSL;

final class CountAggregationConverter {

	static CommonAggregationSelect<Integer> convert(
			BuiltInAggregations.Count aggregation,
			AggregationConversionContext context
	) {
		ExtractingSqlSelect<Object> countSelect = rootSelect(aggregation.column(), context);
		List<ExtractingSqlSelect<Object>> distinctSelects = aggregation.distinctBy().stream()
				.map(column -> rootSelect(column, context))
				.toList();
		List<SingleColumnSqlSelect> rootSelects = Stream.concat(Stream.of(countSelect), distinctSelects.stream())
				.distinct()
				.map(SingleColumnSqlSelect.class::cast)
				.toList();

		String aggregationSource = context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		Field<Integer> count = distinctSelects.isEmpty()
				? DSL.count(countSelect.qualify(aggregationSource).select())
				: DSL.countDistinct(distinctSelects.stream()
				.map(select -> select.qualify(aggregationSource).select())
				.toArray(Field<?>[]::new));
		String[] requiredColumns = rootSelects.stream()
				.flatMap(select -> select.requiredColumns().stream())
				.distinct()
				.toArray(String[]::new);
		FieldWrapper<Integer> groupedCount = new FieldWrapper<>(
				DSL.nullif(count, 0).as(context.alias()),
				requiredColumns
		);

		return CommonAggregationSelect.<Integer>builder()
				.rootSelects(rootSelects)
				.groupBy(groupedCount)
				.build();
	}


	private static ExtractingSqlSelect<Object> rootSelect(
			ResolvedColumn column,
			AggregationConversionContext context
	) {
		return new ExtractingSqlSelect<>(context.tables().getRootTable(), column.physicalName(), Object.class);
	}
}
