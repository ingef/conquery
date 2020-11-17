package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;
import static com.bakdata.conquery.io.result.arrow.QueryToArrowStreamRenderer.generateFieldsFromIdMapping;
import static com.bakdata.conquery.io.result.arrow.QueryToArrowStreamRenderer.generateFieldsFromResultType;
import static com.bakdata.conquery.io.result.arrow.QueryToArrowStreamRenderer.generateWriterPipeline;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.FieldVector;
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
import org.junit.jupiter.api.Test;

@Slf4j
public class ArrowResultGenerationTest {

	private static final int BATCH_SIZE = 1;

	@Test
	void generateSchemaIdMapping() {
		final IdMappingConfig idMapping = new IdMappingConfig() {

			@Getter
			String[] printIdFields = new String[] { "id1", "id2" };

			@Override
			public IdMappingAccessor[] getIdAccessors() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String[] getHeader() {
				throw new UnsupportedOperationException();
			}

		};

		List<Field> fields = generateFieldsFromIdMapping(idMapping.getPrintIdFields());
		
		assertThat(fields).containsExactlyElementsOf(
			List.of(
				new Field("id1", FieldType.nullable(new ArrowType.Utf8()), null),
				new Field("id2", FieldType.nullable(new ArrowType.Utf8()), null)));

	}

	@Test
	void generateSchemaValues() throws IOException {
		List<ResultInfo> resultInfos = Arrays.stream(ResultType.values()).map(TypedSelectDummy::new)
			.map(select -> new SelectResultInfo(select, new CQConcept())).collect(Collectors.toList());

		List<Field> fields = generateFieldsFromResultType(
			resultInfos,
			// Custom column namer so we don't require a dataset registry
			new PrintSettings(false, Locale.ROOT, null, (selectInfo, datasetRegistry) -> selectInfo.getSelect().getLabel()));

		assertThat(fields).containsExactlyElementsOf(
			List.of(
				new Field("BOOLEAN", FieldType.nullable(ArrowType.Bool.INSTANCE), null),
				new Field("INTEGER", FieldType.nullable(new ArrowType.Int(32, true)), null),
				new Field("NUMERIC", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
				new Field("CATEGORICAL", FieldType.nullable(new ArrowType.Utf8()), null),
				new Field("RESOLUTION", FieldType.nullable(new ArrowType.Utf8()), null),
				new Field("DATE", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
				new Field("STRING", FieldType.nullable(new ArrowType.Utf8()), null),
				new Field("MONEY", FieldType.nullable(new ArrowType.Int(32, true)), null)));

		VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, null), ROOT_ALLOCATOR);

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ArrowWriter writer = new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output);

		List<ContainedEntityResult> results = List.of(
			new SinglelineContainedEntityResult(1,
				new Object[] { Boolean.TRUE, 2345634, 123423.34, "CAT1", DateContextMode.DAYS.toString(), 5646, "test_string", 4521 }),
			new SinglelineContainedEntityResult(2, new Object[] { Boolean.FALSE, null, null, null, null, null, null, null }));

		log.info("Starting result write");
		writer.start();
		RowConsumer pipeline = generateWriterPipeline(root, 0, resultInfos.size());
		int batchLineCount = 0;
		for (int resultCount = 0; resultCount < results.size(); resultCount++) {
			root.setRowCount(BATCH_SIZE);
			EntityResult result = results.get(resultCount);
			for (Object[] line : result.asContained().listResultLines()) {
				pipeline.accept(batchLineCount, line);
				batchLineCount++;
			}
			if (batchLineCount >= BATCH_SIZE) {
				writer.writeBatch();
				batchLineCount = 0;
			}
		}
		log.info("Writing final batch");
		if (batchLineCount > 0) {
			writer.writeBatch();
		}

		log.info("Finishing result write");
		writer.end();
		writer.close();

		log.info("Reading the produced arrow data.");
		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());
		try (ArrowStreamReader arrowReader = new ArrowStreamReader(inputStream, ROOT_ALLOCATOR)) {
			VectorSchemaRoot readRoot = arrowReader.getVectorSchemaRoot();
			StringBuilder sb = new StringBuilder();
			sb.append(readRoot.getSchema().getFields().stream().map(Field::getName).collect(Collectors.joining("\t"))).append("\n");
			readRoot.setRowCount(BATCH_SIZE);
			while (arrowReader.loadNextBatch()) {
				List<FieldVector> vectors = readRoot.getFieldVectors();
				for (int rowI = 0; rowI < readRoot.getRowCount(); rowI++) {
					final int currentRow = rowI;
					sb.append(
						vectors.stream().map(vec -> vec.getObject(currentRow)).map(o -> o == null ? "null" : o).map(Object::toString)
							.collect(Collectors.joining("\t")))
						.append("\n");
				}
			}

			String expected = results.stream().map(
				(res) -> res.listResultLines().stream().map(
					line -> Arrays.stream(line).map(o -> o == null ? "null" : o).map(Object::toString).collect(Collectors.joining("\t")))
					.collect(Collectors.joining("\n")))
				.collect(Collectors.joining("\n"));

			assertThat(sb.toString()).isEqualTo(
				Arrays.stream(ResultType.values()).map(Enum::toString).collect(Collectors.joining("\t")) + "\n" + expected + "\n");
		}

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

	// private static Schema generateSchemaFromInternalType(@NonNull
	// List<ResultInfo> infos, PrintSettings settings) {
	//
	// ImmutableList.Builder<Field> fields = ImmutableList.builder();
	//
	// for (ResultInfo info : infos) {
	// MajorTypeId internalType = info.getInternalType();
	// switch (internalType) {
	// case BOOLEAN:
	// fields.add(new Field(info.getUniqueName(settings),
	// FieldType.nullable(ArrowType.Bool.INSTANCE), null));
	// break;
	// case DATE:
	// fields.add(NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings)));
	// break;
	// case DATE_RANGE:
	// fields.add(
	// new Field(info.getUniqueName(settings),
	// FieldType.nullable(ArrowType.Struct.INSTANCE),
	// ImmutableList.of(NAMED_FIELD_DATE_DAY.apply("begin"),
	// NAMED_FIELD_DATE_DAY.apply("end"))));
	// break;
	// case DECIMAL:
	// // Not sure at the moment how to determine the right scale and precision
	// fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new
	// ArrowType.Decimal(0, 0)), null));
	// break;
	// case INTEGER:
	// fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new
	// ArrowType.Int(32, true)), null));
	// break;
	// case MONEY:
	// fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new
	// ArrowType.Decimal(2, 0)), null));
	// break;
	// case REAL:
	// fields.add(
	// new Field(info.getUniqueName(settings),
	// FieldType.nullable(new
	// ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null));
	// break;
	// case STRING:
	// fields.add(new Field(info.getUniqueName(settings), FieldType.nullable(new
	// ArrowType.Utf8()), null));
	// break;
	// default:
	// throw new IllegalStateException("Unknown column type " + internalType);
	// }
	//
	// }
	//
	// return new Schema(fields.build(), null);
	//
	// }

}
