package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowRenderer.*;
import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.Test;

@Slf4j
public class ArrowResultGenerationTest {

	private static final int BATCH_SIZE = 1;
	final IdMappingConfig idMapping = new IdMappingConfig() {

		@Getter
		String[] printIdFields = new String[]{"id1", "id2"};

		@Override
		public IdMappingAccessor[] getIdAccessors() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String[] getHeader() {
			throw new UnsupportedOperationException();
		}

	};

	@Test
	void generateFieldsIdMapping() {

		List<Field> fields = generateFieldsFromIdMapping(idMapping.getPrintIdFields());

		assertThat(fields).containsExactlyElementsOf(
				List.of(
						new Field("id1", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("id2", FieldType.nullable(new ArrowType.Utf8()), null)
				));

	}

	@Test
	void generateFieldsValue() {
		List<ResultInfo> resultInfos = Arrays.stream(ResultType.values()).map(TypedSelectDummy::new)
											 .map(select -> new SelectResultInfo(select, new CQConcept())).collect(Collectors.toList());

		List<Field> fields = generateFieldsFromResultType(
				resultInfos,
				// Custom column namer so we don't require a dataset registry
				new PrintSettings(false, Locale.ROOT, null, (selectInfo, datasetRegistry) -> selectInfo.getSelect().getLabel())
		);

		assertThat(fields).containsExactlyElementsOf(
				List.of(
						new Field("BOOLEAN", FieldType.nullable(ArrowType.Bool.INSTANCE), null),
						new Field("INTEGER", FieldType.nullable(new ArrowType.Int(32, true)), null),
						new Field("NUMERIC", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
						new Field("CATEGORICAL", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("RESOLUTION", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("DATE", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
						new Field("STRING", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("MONEY", FieldType.nullable(new ArrowType.Int(32, true)), null),
						new Field("IDENTIFIER", FieldType.nullable(new ArrowType.Utf8()), null)
				));

	}

	@Test
	void writeAndRead() throws IOException {
		// Prepare every input data
		PrintSettings printSettings = new PrintSettings(false, Locale.ROOT, null, (selectInfo, datasetRegistry) -> selectInfo.getSelect().getLabel());
		List<EntityResult> results = List.of(
				new SinglelineContainedEntityResult(1, new Object[]{Boolean.TRUE, 2345634, 123423.34, "CAT1", DateContext.Resolution.DAYS.toString(), 5646, "test_string", 4521, "a"}),
				new SinglelineContainedEntityResult(2, new Object[]{Boolean.FALSE, null, null, null, null, null, null, null, null}),
				new MultilineContainedEntityResult(3, List.of(
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, "a"},
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, 4, "b"}
				)),
				EntityResult.notContained()
		);

		ManagedQuery mquery = new ManagedQuery(null, null, null) {
			public ResultInfoCollector collectResultInfos() {
				ResultInfoCollector coll = new ResultInfoCollector();
				coll.addAll(Arrays.stream(ResultType.values())
								  .map(TypedSelectDummy::new)
								  .map(select -> new SelectResultInfo(select, new CQConcept()))
								  .collect(Collectors.toList()));
				return coll;
			}

			;

			public List<EntityResult> getResults() {
				return new ArrayList<>(results);
			}
		};

		// First we write to the buffer, than we read from it and parse it as TSV
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		renderToStream(
				(root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				printSettings,
				mquery,
				(cer) -> new String[]{Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())},
				idMapping.getPrintIdFields(),
				BATCH_SIZE
		);

		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());

		String computed = readTSV(inputStream);

		assertThat(computed).isNotBlank();
		assertThat(computed).isEqualTo(generateExpectedTSV(results));

	}

	private static String readTSV(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (ArrowStreamReader arrowReader = new ArrowStreamReader(inputStream, ROOT_ALLOCATOR)) {
			log.info("Reading the produced arrow data.");
			VectorSchemaRoot readRoot = arrowReader.getVectorSchemaRoot();
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
		}
		return sb.toString();
	}

	private String generateExpectedTSV(List<EntityResult> results) {
		String expected = results.stream()
								 .filter(EntityResult::isContained)
								 .map(ContainedEntityResult.class::cast)
								 .map(
										 (res) -> res.listResultLines().stream().map(
												 line -> res.getEntityId() + "\t" + res.getEntityId() + "\t" + Arrays.stream(line)
																													 .map(o -> o == null ? "null" : o)
																													 .map(Object::toString)
																													 .collect(Collectors.joining("\t")))
													 .collect(Collectors.joining("\n")))
								 .collect(Collectors.joining("\n"));

		return Arrays.stream(idMapping.getPrintIdFields()).collect(Collectors.joining("\t")) + "\t" +
			   Arrays.stream(ResultType.values()).map(Enum::toString).collect(Collectors.joining("\t")) + "\n" + expected + "\n";
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
}
