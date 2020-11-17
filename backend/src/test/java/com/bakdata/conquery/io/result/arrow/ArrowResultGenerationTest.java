package com.bakdata.conquery.io.result.arrow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.DateDayVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.Text;
import org.junit.jupiter.api.Test;

@Slf4j
public class ArrowResultGenerationTest {


//	@Test
//	void test() {
//		ManagedQuery mQuery = new ManagedQuery(null, null, null);
//
//		List<EntityResult> results = mQuery.getResults();
//		List<ColumnDescriptor> columnDes = mQuery.getColumnDescriptions();
//		ResultInfoCollector infos = mQuery.collectResultInfos();
//
//	}

	@Test
	void generateSchema() throws IOException {
		Schema schema = generateSchemaFromResultType(
			Arrays.stream(ResultType.values())
				.map(TypedSelectDummy::new)
				.map(select -> new SelectResultInfo(select, new CQConcept()))
				.collect(Collectors.toList())
			,
			// Custom column namer so we don't require a dataset registry
			new PrintSettings(false, Locale.ROOT, null, (selectInfo, datasetRegistry) -> selectInfo.getSelect().getLabel()));

		assertThat(schema).isEqualTo(new Schema(List.of(
			new Field("BOOLEAN", FieldType.nullable(ArrowType.Bool.INSTANCE), null),
			new Field("INTEGER", FieldType.nullable(new ArrowType.Int(32, true)), null),
			new Field("NUMERIC", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
			new Field("CATEGORICAL", FieldType.nullable(new ArrowType.Utf8()), null),
			new Field("RESOLUTION", FieldType.nullable(new ArrowType.Utf8()), null),
			new Field("DATE", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
			new Field("STRING", FieldType.nullable(new ArrowType.Utf8()), null),
			new Field("MONEY", FieldType.nullable(new ArrowType.Int(32, true)), null)
			))
			);

		RootAllocator allocator = new RootAllocator();
		VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator);
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
//		ArrowFileWriter writer = new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), new WritableByteChannel() {
//
//			@Override
//			public boolean isOpen() {
//				return true;
//			}
//
//			@Override
//			public void close() throws IOException {}
//
//			@Override
//			public int write(ByteBuffer src) throws IOException {
//				int cnt = 0;
//				while (src.hasRemaining()) {
//					output.write(src.get());
//					cnt++;
//				}
//				log.info("Wrote {} bytes", cnt);
//				return cnt;
//			}
//
//		});
		
		ArrowWriter writer = new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output);

		List<ContainedEntityResult> results = List.of(new SinglelineContainedEntityResult(
			1, new Object[] { Boolean.TRUE, 2345634, 123423.34, "CAT1", DateContextMode.DAYS.toString(), 5646, "test_string", 4521  }));

		log.info("Starting result write");
		writer.start();
		RowConsumer pipeline = generateWriterPipeline(root);
		root.setRowCount(1);
		int lineCount = 0;
		for (int resultCount = 0; resultCount < results.size(); resultCount++) {
			EntityResult result = results.get(resultCount);
			for (Object[] line : result.asContained().listResultLines()) {
				pipeline.accept(lineCount, line);
			}
		}
		log.info("Writing batch");
		writer.writeBatch();

		log.info("Finishing result write");
		writer.end();
		writer.close();

		log.info("Reading the produced arrow data.");
		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());
		try( ArrowStreamReader arrowReader = new ArrowStreamReader(inputStream, allocator)) {
			VectorSchemaRoot readRoot = arrowReader.getVectorSchemaRoot();
			StringBuilder sb = new StringBuilder();
			while(arrowReader.loadNextBatch()) {
				sb.append(readRoot.contentToTSVString());
			}
			
			String expected = results.stream()
				.map((res) -> res.listResultLines().stream()
					.map(line -> Arrays.stream(line)
						.map(Object::toString)
						.collect(Collectors.joining("\t"))
						)
					.collect(Collectors.joining("\n")))
				.collect(Collectors.joining("\n"));
			
			assertThat(sb.toString()).isEqualTo(Arrays.stream(ResultType.values()).map(Enum::toString).collect(Collectors.joining("\t")) + "\n" + expected + "\n");
		}
		

		
	}

	private final static Function<String, Field> NAMED_FIELD_DATE_DAY = (name) -> new Field(name,
		FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
	
	private static RowConsumer generateWriterPipeline(VectorSchemaRoot root){
		RowConsumer start = (n, r) -> {};
		for (int vecI = 0; vecI < root.getFieldVectors().size(); vecI++) {
			final int pos = vecI;
			final FieldVector vector = root.getVector(vecI);
			
			if(vector instanceof IntVector) {
				start = start.andThen((rowNumber, line) -> ((IntVector)vector).setSafe(rowNumber, (int) line[pos]));
				continue;
			}

			if(vector instanceof VarCharVector) {
				start = start.andThen((rowNumber, line) -> ((VarCharVector)vector).setSafe(rowNumber, new Text((String) line[pos])));
				continue;
			}
			
			if(vector instanceof BitVector) {
				start = start.andThen((rowNumber, line) -> ((BitVector)vector).setSafe(rowNumber, ((Boolean) line[pos])? 1 : 0));
				continue;
			}
			
			if(vector instanceof Float4Vector) {
				start = start.andThen((rowNumber, line) -> ((Float4Vector)vector).setSafe(rowNumber, ((Number) line[pos]).floatValue()));
				continue;
			}
			
			if(vector instanceof Float8Vector) {
				start = start.andThen((rowNumber, line) -> ((Float8Vector)vector).setSafe(rowNumber, ((Number) line[pos]).doubleValue()));
				continue;
			}
			
			if(vector instanceof DateDayVector) {
				start = start.andThen((rowNumber, line) -> ((DateDayVector)vector).setSafe(rowNumber, ((Number) line[pos]).intValue()));
				continue;
			}
			
			throw new UnsupportedOperationException("Vector type for writing result: "+ vector.getClass());
		}
		return start;
		
	}
	
	private static Schema generateSchemaFromResultType(@NonNull List<ResultInfo> infos, PrintSettings settings) {
		
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
		
		return new Schema(fields.build(), null);
		
	}
	
	private static class TypedSelectDummy extends Select {
		private final ResultType resultType;
		
		public TypedSelectDummy(ResultType resultType) {
			this.setLabel(resultType.toString());
			this.resultType = resultType;
		}

		@Override
		public Aggregator<String> createAggregator() {
			return new Aggregator<String>() {

				@Override
				public Aggregator<String> doClone(CloneContext ctx) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void acceptEvent(Bucket bucket, int event) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getAggregationResult() {
					throw new UnsupportedOperationException();
				}

				@Override
				public ResultType getResultType() {
					return resultType;
				}
				
			};
		}
		
	}
	


//	private static Schema generateSchemaFromInternalType(@NonNull List<ResultInfo> infos, PrintSettings settings) {
//
//		ImmutableList.Builder<Field> fields = ImmutableList.builder();
//
//		for (ResultInfo info : infos) {
//			MajorTypeId internalType = info.getInternalType();
//			switch (internalType) {
//				case BOOLEAN:
//					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Bool.INSTANCE), null));
//					break;
//				case DATE:
//					fields.add(NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings)));
//					break;
//				case DATE_RANGE:
//					fields.add(
//						new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Struct.INSTANCE),
//							ImmutableList.of(NAMED_FIELD_DATE_DAY.apply("begin"), NAMED_FIELD_DATE_DAY.apply("end"))));
//					break;
//				case DECIMAL:
//					// Not sure at the moment how to determine the right scale and precision
//					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Decimal(0, 0)), null));
//					break;
//				case INTEGER:
//					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null));
//					break;
//				case MONEY:
//					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Decimal(2, 0)), null));
//					break;
//				case REAL:
//					fields.add(
//						new Field(info.getUniqueName(settings),
//							FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null));
//					break;
//				case STRING:
//					fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Utf8()), null));
//					break;
//				default:
//					throw new IllegalStateException("Unknown column type " + internalType);
//			}
//
//		}
//
//		return new Schema(fields.build(), null);
//
//	}

}
