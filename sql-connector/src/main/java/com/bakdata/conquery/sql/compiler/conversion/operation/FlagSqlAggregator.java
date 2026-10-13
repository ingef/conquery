package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.CommonAggregationSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInAggregations;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import org.jooq.Condition;
import org.jooq.Field;

final class FlagSqlAggregator {
	static CommonAggregationSelect<?> convert(BuiltInAggregations.Flags aggregation, AggregationConversionContext context) {
		Map<String, SingleColumnSqlSelect> rootSelects = createFlagRootSelectMap(aggregation, context.tables().getRootTable());
		return aggregation(rootSelects, createFlagSelect(context.alias(), context.tables(), context.dialect(), rootSelects));
	}

	private static <T> CommonAggregationSelect<T> aggregation(Map<String, SingleColumnSqlSelect> roots, FieldWrapper<T> grouped) {
		return CommonAggregationSelect.<T>builder().rootSelects(roots.values()).groupBy(grouped).build();
	}
	private static Map<String, SingleColumnSqlSelect> createFlagRootSelectMap(BuiltInAggregations.Flags flagSelect, String rootTable) {
		return flagSelect.columns()
						 .entrySet().stream()
						 .collect(Collectors.toMap(
								 Map.Entry::getKey,
								 entry -> {
									 ResolvedColumn column = entry.getValue();
									 Field<Object> field = field(name(rootTable, column.physicalName()));
									 return new FieldWrapper<>(field.as(column.physicalName()), column.physicalName()
									 );
								 }
						 ));
	}

	private static FieldWrapper<?> createFlagSelect(
			String alias,
			SqlTables connectorTables,
			CompilerDialect functionProvider,
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

}
