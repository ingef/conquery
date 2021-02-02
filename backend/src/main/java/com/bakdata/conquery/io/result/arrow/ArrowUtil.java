package com.bakdata.conquery.io.result.arrow;

import java.util.function.Function;

import lombok.experimental.UtilityClass;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

@UtilityClass
public class ArrowUtil {
	public final static RootAllocator ROOT_ALLOCATOR = new RootAllocator();

	public final static Function<String, Field> NAMED_FIELD_DATE_DAY = (name) -> new Field(name, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);

}
