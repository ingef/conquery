package com.bakdata.conquery.io.result.arrow;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.experimental.UtilityClass;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

@UtilityClass
public class ArrowUtil {
	public final static RootAllocator ROOT_ALLOCATOR = new RootAllocator();

	public final static Function<String, Field> NAMED_FIELD_DATE_DAY = (name) -> new Field(name, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);


	private final static Map<Class<? extends ResultType>, FieldCreatorFactory> FIELD_MAP = Map.of(
			ResultType.BooleanT.class, ArrowUtil::boolField,
			ResultType.IntegerT.class, ArrowUtil::integerField,
			ResultType.NumericT.class, ArrowUtil::floatField,
			ResultType.DateT.class, ArrowUtil::dateField,
			ResultType.DateRangeT.class, ArrowUtil::dateRangeField,
			ResultType.MoneyT.class, ArrowUtil::integerField,
			ResultType.ListT.class, ArrowUtil::listField
	);

	private interface FieldCreatorFactory extends BiFunction<ResultInfo, PrintSettings, Field> {
	}

	private static Field stringField(ResultInfo info, PrintSettings settings) {
		return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null);
	}

	private static Field boolField(ResultInfo info, PrintSettings settings) {
		return new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Bool.INSTANCE), null);
	}

	private static Field integerField(ResultInfo info, PrintSettings settings) {
		return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null);
	}

	private static Field floatField(ResultInfo info, PrintSettings settings) {
		return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null);
	}

	private static Field dateField(ResultInfo info, PrintSettings settings) {
		return NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings));
	}

	private static Field dateRangeField(ResultInfo info, PrintSettings settings) {
		return new Field(
				info.getUniqueName(settings),
				FieldType.nullable(ArrowType.Struct.INSTANCE),
				List.of(
						NAMED_FIELD_DATE_DAY.apply("min"),
						NAMED_FIELD_DATE_DAY.apply("max")
				));
	}

	private static Field listField(ResultInfo info, PrintSettings settings) {
		if (!(info.getType() instanceof ResultType.ListT)) {
			throw new IllegalStateException("Expected result type of " + ResultType.ListT.class.getName() + " but got " + info.getType().getClass().getName());
		}

		final ResultType elementType = ((ResultType.ListT) info.getType()).getElementType();
		FieldCreatorFactory nestedfieldCreatorFactory = FIELD_MAP.getOrDefault(elementType.getClass(), ArrowUtil::stringField);
		final Field nestedField = nestedfieldCreatorFactory.apply(info, settings);
		return new Field(
				info.getUniqueName(settings),
				FieldType.nullable(ArrowType.List.INSTANCE),
				List.of(nestedField)
		);
	}

	/**
	 * Creates an arrow field vector (a column) corresponding to the internal conquery type and initializes the column with
	 * a localized header.
	 * @param info internal meta data for the result column
	 * @param settings settings for the overall creation of the result
	 * @return a Field (the arrow representation of the column)
	 */
	public Field createField(ResultInfo info, PrintSettings settings) {
		// Fallback to string field if type is not explicitly registered
		FieldCreatorFactory fieldCreatorFactory = FIELD_MAP.getOrDefault(info.getType().getClass(), ArrowUtil::stringField);
		return fieldCreatorFactory.apply(info, settings);
	}
}
