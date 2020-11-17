package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowUtil.NAMED_FIELD_DATE_DAY;
import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.util.Preconditions;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.DateDayVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.Text;

@Slf4j
public class QueryToArrowStreamRenderer {
	

	private static final int BATCH_SIZE = 10;
	
	public static void renderToStream(OutputStream out, PrintSettings cfg, ManagedQuery query, Function<ContainedEntityResult,String[]> idMapper, String[] idHeaders) throws IOException {

		// Combine id and value Fields to one vector to build a schema
		List<Field> fields = new ArrayList<>(generateFieldsFromIdMapping(idHeaders));
		List<ResultInfo> resultInfos = query.collectResultInfos().getInfos();
		fields.addAll(generateFieldsFromResultType(resultInfos, cfg));
		VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, null), ROOT_ALLOCATOR);
		
		// Build separate pipelines for id and value, as they have different sources but the same target
		RowConsumer idPipeline = generateWriterPipeline(root, 0, idHeaders.length);
		RowConsumer valuePipeline = generateWriterPipeline(root, idHeaders.length, resultInfos.size());

		
		List<ContainedEntityResult> results = query.getResults().stream().filter(ContainedEntityResult.class::isInstance).map(ContainedEntityResult.class::cast).collect(Collectors.toList());

		// Write the data
		try(ArrowWriter writer = new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), out)) {			
			write(writer, root, idPipeline, valuePipeline, idMapper, results);
		}
		
	}

	public static void write(ArrowWriter writer, VectorSchemaRoot root, RowConsumer idPipeline, RowConsumer valuePipeline, Function<ContainedEntityResult,String[]> idMapper, List<ContainedEntityResult> results) throws IOException {
		log.info("Starting result write");
		writer.start();
		int batchLineCount = 0;
		root.setRowCount(BATCH_SIZE);
		for (int resultCount = 0; resultCount < results.size(); resultCount++) {
			ContainedEntityResult result = results.get(resultCount);
			for (Object[] line : result.listResultLines()) {
				// Write id information
				idPipeline.accept(batchLineCount, idMapper.apply(result));
				// Write values
				valuePipeline.accept(batchLineCount, line);
				batchLineCount++;
			}
			if(batchLineCount >= BATCH_SIZE) {				
				writer.writeBatch();
				batchLineCount = 0;
			}
		}
		log.info("Writing final batch");
		if(batchLineCount > 0) {
			root.setRowCount(batchLineCount);
			writer.writeBatch();
		}
		log.info("Finishing result write");
		writer.end();
	}
	
	private static RowConsumer intVectorFiller(IntVector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, (int) line[pos]);
		};
	}
	
	private static RowConsumer bitVectorFiller(BitVector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, ((Boolean) line[pos])? 1 : 0);
		};
	}
	
	private static RowConsumer float8VectorFiller(Float8Vector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, ((Number) line[pos]).doubleValue());
		};
	}
	
	private static RowConsumer float4VectorFiller(Float4Vector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, ((Number) line[pos]).floatValue());
		};
	}
	
	private static RowConsumer varCharVectorFiller(VarCharVector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, new Text((String) line[pos]));
		};
	}
	
	private static RowConsumer dateDayVectorFiller(DateDayVector vector, int pos) {
		return (rowNumber, line) -> {
			if (line[pos] == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, ((Number) line[pos]).intValue());
		};
	}

	
	public static RowConsumer generateWriterPipeline(VectorSchemaRoot root, int vectorOffset, int numVectors){
		Preconditions.checkArgument(vectorOffset >= 0, "Offset was negativ: %s", vectorOffset);
		Preconditions.checkArgument(numVectors >= 0, "Number of vectors was negativ: %s", numVectors);
		
		RowConsumer start = (n, r) -> {};
		for (int vecI = vectorOffset, resultPos = 0; vecI < root.getFieldVectors().size() && vecI < vectorOffset + numVectors; vecI++, resultPos++) {
			final int pos = resultPos;
			final FieldVector vector = root.getVector(vecI);
			
			if(vector instanceof IntVector) {
				start = start.andThen(intVectorFiller((IntVector) vector, pos));
				continue;
			}

			if(vector instanceof VarCharVector) {
				start = start.andThen(varCharVectorFiller((VarCharVector) vector, pos));
				continue;
			}
			
			if(vector instanceof BitVector) {
				start = start.andThen(bitVectorFiller((BitVector) vector, pos));
				continue;
			}
			
			if(vector instanceof Float4Vector) {
				start = start.andThen(float4VectorFiller((Float4Vector)vector, pos));
				continue;
			}
			
			if(vector instanceof Float8Vector) {
				start = start.andThen(float8VectorFiller((Float8Vector)vector, pos));
				continue;
			}
			
			if(vector instanceof DateDayVector) {
				start = start.andThen(dateDayVectorFiller((DateDayVector) vector, pos));
				continue;
			}
			
			throw new UnsupportedOperationException("Vector type for writing result: "+ vector.getClass());
		}
		return start;
		
	}
	
	public static List<Field> generateFieldsFromIdMapping(String[] idHeaders){
		Preconditions.checkArgument(idHeaders != null && idHeaders.length > 0, "No id headers given");

		ImmutableList.Builder<Field> fields = ImmutableList.builder();
		
		for(String header : idHeaders) {
			fields.add(new Field(header, FieldType.nullable(new ArrowType.Utf8()), null));
		}
		
		return fields.build();
	}
	
	public static List<Field> generateFieldsFromResultType(@NonNull List<ResultInfo> infos, PrintSettings settings) {
		
		ImmutableList.Builder<Field> fields = ImmutableList.builder();
		
		for(ResultInfo info : infos) {
			switch(info.getType()) {
				case BOOLEAN:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Bool.INSTANCE), null));
					break;
				case CATEGORICAL:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null));
					break;
				case DATE:
					fields.add(NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings)));
					break;
				case INTEGER:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null));
					break;
				case MONEY:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null));
					break;
				case NUMERIC:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null));
					break;
				case RESOLUTION:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null));
					break;
				case STRING:
					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null));
					break;
				default:
					throw new IllegalStateException("Unknown column type " + info.getType());
				
			}
			
		}
		
		return fields.build();
		
	}
}
