package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowUtil.NAMED_FIELD_DATE_DAY;
import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
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
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.Text;

@Slf4j
public class ArrowRenderer {
	
	public static void renderToStream(
		Function<VectorSchemaRoot, ArrowWriter> writerProducer, 
		PrintSettings cfg,
		ManagedExecution<?> exec, 
		Function<ContainedEntityResult,String[]> idMapper, 
		String[] idHeaders,	
		int batchsize) throws IOException
	{
		// Test the execution if the result is renderable into one table
		Stream<EntityResult> results = getResults(exec);
		List<ResultInfo> resultInfos = getResultInfos(exec);
		
		// Combine id and value Fields to one vector to build a schema
		List<Field> fields = new ArrayList<>(generateFieldsFromIdMapping(idHeaders));
		fields.addAll(generateFieldsFromResultType(resultInfos, cfg));
		VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, null), ROOT_ALLOCATOR);
		
		// Build separate pipelines for id and value, as they have different sources but the same target
		RowConsumer[] idWriters = generateWriterPipeline(root, 0, idHeaders.length);
		RowConsumer[] valueWriter = generateWriterPipeline(root, idHeaders.length, resultInfos.size());

		// Write the data
		try(ArrowWriter writer = writerProducer.apply(root)) {			
			write(writer, root, idWriters, valueWriter, idMapper, results, batchsize);
		}
		
	}
	
	private static Stream<EntityResult> getResults(ManagedExecution<?> exec) {
		Stream<EntityResult> results;
		if(exec instanceof ManagedQuery) {
			results = ((ManagedQuery)exec).getResults().stream();
		}
		else if(exec instanceof ManagedForm && ((ManagedForm)exec).getSubQueries().size() == 1) {
			results = ((ManagedForm)exec).getSubQueries().values().iterator().next().stream().flatMap(mq -> mq.getResults().stream());
		}
		else {
			throw new IllegalStateException("The provided execution cannot be rendered as a single table. Was: " + exec.getId());
		}
		return results;
	}
	
	private static List<ResultInfo> getResultInfos(ManagedExecution<?> exec) {
		List<ResultInfo> resultInfos;
		if(exec instanceof ManagedQuery) {
			resultInfos = ((ManagedQuery)exec).collectResultInfos().getInfos();
		}
		else if(exec instanceof ManagedForm && ((ManagedForm)exec).getSubQueries().size() == 1) {
			resultInfos = ((ManagedForm)exec).getSubQueries().values().iterator().next().get(0).collectResultInfos().getInfos();
		}
		else {
			throw new IllegalStateException("The provided execution cannot be rendered as a single table. Was: " + exec.getId());
		}
		return resultInfos;
	}
	

	public static void write(
		ArrowWriter writer, 
		VectorSchemaRoot root, 
		RowConsumer[] idWriter, 
		RowConsumer[] valueWriter, 
		Function<ContainedEntityResult,String[]> idMapper, 
		Stream<EntityResult> results,
		int batchSize) throws IOException 
	{
		Preconditions.checkArgument(batchSize > 0, "Batchsize needs be larger than 0.");
		// TODO add time metric for writing
		
		log.trace("Starting result write");
		writer.start();
		int batchCount = 0;
		int batchLineCount = 0;
		root.setRowCount(batchSize);
		Iterator<EntityResult> resultIter = results.iterator();
		while(resultIter.hasNext()) {
			EntityResult result = resultIter.next();
			if (!result.isContained()) {
				continue;
			}
			ContainedEntityResult cer = result.asContained();
			for (Object[] line : cer.listResultLines()) {
				for(int cellIndex = 0; cellIndex < idWriter.length; cellIndex++) {
					// Write id information
					idWriter[cellIndex].accept(batchLineCount, idMapper.apply(cer));
				}
				for(int cellIndex = 0; cellIndex < valueWriter.length; cellIndex++) {
					// Write values
					valueWriter[cellIndex].accept(batchLineCount, line);					
				}
				batchLineCount++;
				
				if(batchLineCount >= batchSize) {				
					writer.writeBatch();
					batchLineCount = 0;
				}
			}
		}
		if(batchLineCount > 0) {
			root.setRowCount(batchLineCount);
			writer.writeBatch();
			batchCount++;
		}
		log.trace("Wrote {} batches of size {} (last batch might be smaller)", batchCount, batchSize);
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
			vector.setSafe(rowNumber, new Text(Objects.toString(line[pos])));
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

	
	public static RowConsumer[] generateWriterPipeline(VectorSchemaRoot root, int vectorOffset, int numVectors){
		Preconditions.checkArgument(vectorOffset >= 0, "Offset was negativ: %s", vectorOffset);
		Preconditions.checkArgument(numVectors >= 0, "Number of vectors was negativ: %s", numVectors);
		
		RowConsumer[] builder = new RowConsumer[numVectors];
		
		for (
			int vecI = vectorOffset, resultPos = 0; 
			( vecI < root.getFieldVectors().size() ) && ( vecI < vectorOffset + numVectors );
			vecI++, resultPos++
			) {
			final int pos = resultPos;
			final FieldVector vector = root.getVector(vecI);

			//TODO When Pattern-matching lands, clean this up. (Think Java 12?)
			if(vector instanceof IntVector) {
				builder[resultPos]  = intVectorFiller((IntVector) vector, pos);
				continue;
			}

			if(vector instanceof VarCharVector) {
				builder[resultPos]  = varCharVectorFiller((VarCharVector) vector, pos);
				continue;
			}
			
			if(vector instanceof BitVector) {
				builder[resultPos]  = bitVectorFiller((BitVector) vector, pos);
				continue;
			}
			
			if(vector instanceof Float4Vector) {
				builder[resultPos]  = float4VectorFiller((Float4Vector)vector, pos);
				continue;
			}
			
			if(vector instanceof Float8Vector) {
				builder[resultPos]  = float8VectorFiller((Float8Vector)vector, pos);
				continue;
			}
			
			if(vector instanceof DateDayVector) {
				builder[resultPos]  = dateDayVectorFiller((DateDayVector) vector, pos);
				continue;
			}
			
			throw new UnsupportedOperationException("Unsupported vector type for writing result: "+ vector.getClass());
		}
		return builder;
		
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
		return  infos.stream()
			 .map(info -> getFieldForResultInfo(info, settings))
			 .collect(Collectors.toUnmodifiableList());
		
	}
	
	// TODO replace with switch expression in java 12
	private static Field getFieldForResultInfo(ResultInfo info, PrintSettings settings){
		switch(info.getType()) {
			case BOOLEAN:
				return new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Bool.INSTANCE), null);
			case CATEGORICAL:
				return  new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null);
			case DATE:
				return NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings));
			case INTEGER:
				return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null);
			case MONEY:
				return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null);
			case NUMERIC:
				return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null);
			case RESOLUTION:
				return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null);
			case STRING:
				return new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null);
			default:
				throw new IllegalStateException("Unknown column type " + info.getType());
			
		}
	}
}
