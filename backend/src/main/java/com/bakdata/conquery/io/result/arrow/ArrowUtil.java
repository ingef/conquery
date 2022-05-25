package com.bakdata.conquery.io.result.arrow;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

@UtilityClass
public class ArrowUtil {

	public final static RootAllocator ROOT_ALLOCATOR = new RootAllocator();

	private final static Map<Class<? extends ResultType>, BiFunction<ResultInfo, String, Field>> FIELD_MAP = Map.of(
			ResultType.BooleanT.class, ArrowUtil::boolField,
			ResultType.IntegerT.class, ArrowUtil::integerField,
			ResultType.NumericT.class, ArrowUtil::floatField,
			ResultType.DateT.class, ArrowUtil::dateField,
			ResultType.DateRangeT.class, ArrowUtil::dateRangeField,
			ResultType.MoneyT.class, ArrowUtil::integerField,
			ResultType.ListT.class, ArrowUtil::listField
	);

	private static Field stringField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Utf8()), null);
	}

	private static Field boolField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(ArrowType.Bool.INSTANCE), null);
	}

	private static Field integerField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Int(32, true)), null);
	}

	private static Field floatField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null);
	}

	private static Field dateField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
	}

	private static Field dateRangeField(ResultInfo info, @NonNull String uniqueName) {
		return new Field(
				uniqueName,
				FieldType.nullable(ArrowType.Struct.INSTANCE),
				List.of(
						dateField(info, "min"),
						dateField(info, "max")
				));
	}

	private static Field listField(ResultInfo info, @NonNull String uniqueName) {
		if (!(info.getType() instanceof ResultType.ListT)) {
			throw new IllegalStateException("Expected result type of " + ResultType.ListT.class.getName() + " but got " + info.getType().getClass().getName());
		}

		final ResultType elementType = ((ResultType.ListT) info.getType()).getElementType();
		BiFunction<ResultInfo, String, Field> nestedFieldCreator = FIELD_MAP.getOrDefault(elementType.getClass(), ArrowUtil::stringField);
		final Field nestedField = nestedFieldCreator.apply(info, uniqueName);
		return new Field(
				uniqueName,
				FieldType.nullable(ArrowType.List.INSTANCE),
				List.of(nestedField)
		);
	}

	/**
	 * Creates an arrow field vector (a column) corresponding to the internal conquery type and initializes the column with
	 * a localized header.
	 * @param info internal meta data for the result column
	 * @param collector to create unique names across the columns
	 * @return a Field (the arrow representation of the column)
	 */
	public Field createField(ResultInfo info, UniqueNamer collector) {
		// Fallback to string field if type is not explicitly registered
		BiFunction<ResultInfo, String, Field> fieldCreator = FIELD_MAP.getOrDefault(info.getType().getClass(), ArrowUtil::stringField);
		return fieldCreator.apply(info, collector.getUniqueName(info));
	}

	public List<ArrowType> extractTypes(Schema schema) {
		return schema.getFields().stream().map(Field::getType).collect(Collectors.toList());
	}
}
