package com.bakdata.conquery.io.result.arrow;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.types.ResultType;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class ArrowUtil {

	public static final RootAllocator ROOT_ALLOCATOR = new RootAllocator();

	private Field fieldFor(ResultType type, String name, PrintSettings settings) {
		if (type instanceof ResultType.ListT<?>) {
			return ArrowUtil.listField(name, type, settings);
		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> ArrowUtil.boolField(name);
			case INTEGER -> ArrowUtil.integerField(name);
			case MONEY -> ArrowUtil.moneyField(name,  settings.getCurrency().getDefaultFractionDigits());
			case NUMERIC -> ArrowUtil.floatField(name);
			case DATE -> ArrowUtil.dateField(name);
			case DATE_RANGE -> ArrowUtil.dateRangeField(name);
			case STRING -> ArrowUtil.stringField(name);
		};
	}


	private static Field stringField(@NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Utf8()), null);
	}

	private static Field boolField(@NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(ArrowType.Bool.INSTANCE), null);
	}

	private static Field integerField(@NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Int(32, true)), null);
	}

	private static Field moneyField(@NonNull String uniqueName, int scale) {
		/*
		 * From https://arrow.apache.org/docs/python/generated/pyarrow.decimal128.html
		 * Maximum precision is 38 digits total, therefore we can assume to pack all digits, minus scale in there;
		 * assuming that no currency will exceed 28 digits, this should be more than fine as a heuristic.
		 */
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Decimal(38 - scale, scale, 128)), null);
	}

	private static Field floatField(@NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null);
	}

	private static Field dateField(@NonNull String uniqueName) {
		return new Field(uniqueName, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
	}

	private static Field dateRangeField(@NonNull String uniqueName) {
		return new Field(
				uniqueName,
				FieldType.nullable(ArrowType.Struct.INSTANCE),
				List.of(
						dateField("min"),
						dateField("max")
				));
	}

	private static Field listField(@NonNull String uniqueName, ResultType type, PrintSettings printSettings) {
		final ResultType elementType = ((ResultType.ListT<?>) type).getElementType();
		final Field nestedField = fieldFor(elementType, uniqueName, printSettings);
		
		return new Field(uniqueName, FieldType.nullable(ArrowType.List.INSTANCE), List.of(nestedField));
	}

	public static List<Field> generateFields(@NonNull List<ResultInfo> info, UniqueNamer collector, PrintSettings printSettings) {
		return info.stream()
				   .map(i -> fieldFor(i.getType(), collector.getUniqueName(i, printSettings), printSettings))
				   .toList();

	}

	@NotNull
	public static List<Field> generateFields(List<ResultInfo> idHeaders, List<ResultInfo> resultInfo, UniqueNamer uniqueNamer, PrintSettings printSettings) {
		// Combine id and value Fields to one vector to build a schema
		List<Field> fields = new ArrayList<>();

		fields.addAll(generateFields(idHeaders, uniqueNamer, printSettings));
		fields.addAll(generateFields(resultInfo, uniqueNamer, printSettings));

		return fields;
	}
}
