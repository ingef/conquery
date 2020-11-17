package com.bakdata.conquery.io.result.arrow;

import java.util.List;
import java.util.function.Function;

import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.experimental.UtilityClass;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

@UtilityClass
public class ArrowUtil {
	public final RootAllocator ROOT_ALLOCATOR = new RootAllocator();

	public final Function<String, Field> NAMED_FIELD_DATE_DAY = (name) -> new Field(name, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
	
	public int calculateRowCount(List<ContainedEntityResult> results) {
		int count = 0;
		for(ContainedEntityResult result : results) {
			if (result instanceof SinglelineEntityResult) {
				// Don't call SinglelineEntityResult::listResultLines() to avoid object creation
				count++;
				continue;
			}
			count += result.listResultLines().size();
		}
		return count;
	}

}
