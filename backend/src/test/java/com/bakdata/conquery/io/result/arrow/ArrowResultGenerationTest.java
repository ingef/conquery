package com.bakdata.conquery.io.result.arrow;

import java.util.List;

import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import org.apache.arrow.flatbuf.Schema;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.Test;

public class ArrowResultGenerationTest {
	
	@Test
	void test() {
		ManagedQuery mQuery = new ManagedQuery(null, null, null);
		
		List<EntityResult> results = mQuery.getResults();
		List<ColumnDescriptor> columnDes = mQuery.getColumnDescriptions();
		ResultInfoCollector infos = mQuery.collectResultInfos();
		
		
	}
	
	private static Schema generateSchema(@NonNull ResultInfoCollector infos, PrintSettings settings) {
		
		ImmutableList.Builder<Field> childrenBuilder = ImmutableList.builder();
		
		for(ResultInfo info : infos.getInfos()) {
			switch(info.getType()) {
				case BOOLEAN:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Bool()), null));
					break;
				case CATEGORICAL:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.LargeUtf8()), null));
					break;
				case DATE:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null));
					break;
				case INTEGER:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null));
					break;
				case MONEY:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Decimal(2, 1)), null));
					break;
				case NUMERIC:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null));
					break;
				case RESOLUTION:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.LargeUtf8()), null));
					break;
				case STRING:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.LargeUtf8()), null));
					break;
				default:
					throw new IllegalStateException("Unknown column type " + info.getType());
					break;
				
			}
			
		}
		
		return new Schema(childrenBuilder.build(), null);
		
	}

}
