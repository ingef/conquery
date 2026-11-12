package com.bakdata.conquery.sql.conversion.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.WindowSpecificationOrderByStep;
import org.jooq.impl.DSL;

@UtilityClass
public class PartitioningUtil {

	public static WindowSpecificationOrderByStep partitionBy(SqlIdColumns ids) {
		return partitionBy(ids, List.of());
	}

	public static WindowSpecificationOrderByStep partitionBy(
			SqlIdColumns ids,
			Collection<? extends Field<?>> additionalFields
	) {
		return DSL.partitionBy(partitioningFields(ids, additionalFields));
	}

	public static FieldWrapper<Integer> rowNumber(SqlIdColumns ids, Collection<? extends Field<?>> additionalFields, String alias) {
		final List<Field<?>> partitioningFields = partitioningFields(ids, additionalFields);
		final String[] requiredColumns = partitioningFields.stream().map(Field::getName).toArray(String[]::new);

		return new FieldWrapper<>(DSL.rowNumber().over(DSL.partitionBy(partitioningFields)).as(alias), requiredColumns);
	}

	private static List<Field<?>> partitioningFields(SqlIdColumns ids, Collection<? extends Field<?>> additionalFields) {
		return Stream.concat(ids.toFields().stream(), additionalFields.stream()).toList();
	}
}
