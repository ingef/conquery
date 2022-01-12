package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultTestUtil.*;
import static com.bakdata.conquery.io.result.arrow.ArrowRenderer.*;
import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.result.ResultTestUtil;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.NonNull;
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
import org.apache.arrow.vector.util.JsonStringArrayList;
import org.junit.jupiter.api.Test;

@Slf4j
public class ArrowResultGenerationTest {

	private static final int BATCH_SIZE = 2;
	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	public static final PrintSettings
			PRINT_SETTINGS =
			new PrintSettings(false, Locale.ROOT, null, CONFIG, null, (selectInfo) -> selectInfo.getSelect().getLabel());

	@Test
	void generateFieldsIdMapping() {

		final UniqueNamer uniqueNamer = new UniqueNamer(PRINT_SETTINGS, ResultTestUtil.ID_FIELDS);

		List<Field> fields = generateFields(ResultTestUtil.ID_FIELDS, uniqueNamer);

		assertThat(fields).containsExactlyElementsOf(
				List.of(
						new Field("id1", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("id2", FieldType.nullable(new ArrowType.Utf8()), null)
				));

	}

	@Test
	void generateFieldsValue() {
		List<ResultInfo> resultInfos = getResultTypes().stream().map(ResultTestUtil.TypedSelectDummy::new)
													   .map(select -> new SelectResultInfo(select, new CQConcept())).collect(Collectors.toList());


		final UniqueNamer uniqueNamer = new UniqueNamer(PRINT_SETTINGS, resultInfos);

		List<Field> fields = generateFields(
				resultInfos,
				// Custom column namer so we don't require a dataset registry
				uniqueNamer
		);

		assertThat(fields).containsExactlyElementsOf(
				List.of(
						new Field("BOOLEAN", FieldType.nullable(ArrowType.Bool.INSTANCE), null),
						new Field("INTEGER", FieldType.nullable(new ArrowType.Int(32, true)), null),
						new Field("NUMERIC", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
						new Field("CATEGORICAL", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("RESOLUTION", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("DATE", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
						new Field(
								"DATE_RANGE",
								FieldType.nullable(ArrowType.Struct.INSTANCE),
								List.of(
										new Field("min", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
										new Field("max", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null)
								)
						),
						new Field("STRING", FieldType.nullable(new ArrowType.Utf8()), null),
						new Field("MONEY", FieldType.nullable(new ArrowType.Int(32, true)), null),
						new Field("LIST[BOOLEAN]", FieldType.nullable(ArrowType.List.INSTANCE), List.of(new Field("LIST[BOOLEAN]", FieldType.nullable(ArrowType.Bool.INSTANCE), null))),
						new Field("LIST[DATE_RANGE]", FieldType.nullable(ArrowType.List.INSTANCE), List.of(new Field(
								"LIST[DATE_RANGE]",
								FieldType.nullable(ArrowType.Struct.INSTANCE),
								List.of(
										new Field("min", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
										new Field("max", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null)
								)
						))),
						new Field("LIST[STRING]", FieldType.nullable(ArrowType.List.INSTANCE), List.of(new Field("LIST[STRING]", FieldType.nullable(new ArrowType.Utf8()), null)))
				)
		);

	}

	@Test
	void writeAndRead() throws IOException {

		// Initialize internationalization
		I18n.init();

		// Prepare every input data
		PrintSettings printSettings = new PrintSettings(
				false,
				Locale.ROOT,
				null,
				CONFIG,
				(cer) -> EntityPrintId.from(Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())),
				(selectInfo) -> selectInfo.getSelect().getLabel()
		);
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		List<EntityResult> results = getTestEntityResults();

		ManagedQuery mquery = getTestQuery();

		// First we write to the buffer, than we read from it and parse it as TSV
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		renderToStream(
				(root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				printSettings,
				BATCH_SIZE,
				ResultTestUtil.ID_FIELDS,
				mquery.getResultInfos(),
				mquery.streamResults()
		);

		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());

		String computed = readTSV(inputStream);

		assertThat(computed).isNotBlank();
		assertThat(computed).isEqualTo(generateExpectedTSV(results, mquery.getResultInfos(), printSettings));

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
							vectors.stream()
								   .map(vec -> vec.getObject(currentRow))
								   .map(ArrowResultGenerationTest::getPrintValue)
								   .collect(Collectors.joining("\t")))
					  .append("\n");
				}
			}
		}
		return sb.toString();
	}

	private String generateExpectedTSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings settings) {
		String expected = results.stream()
								 .map(EntityResult.class::cast)
								 .map(res -> {
									 StringJoiner lineJoiner = new StringJoiner("\n");

									 for (Object[] line : res.listResultLines()) {
										 StringJoiner valueJoiner = new StringJoiner("\t");
										 valueJoiner.add(String.valueOf(res.getEntityId()));
										 valueJoiner.add(String.valueOf(res.getEntityId()));
										 for (int lIdx = 0; lIdx < line.length; lIdx++) {
											 Object val = line[lIdx];
											 ResultInfo info = resultInfos.get(lIdx);
											 valueJoiner.add(getPrintValue(val, info.getType(), settings));
										 }
										 lineJoiner.add(valueJoiner.toString());
									 }
									 return lineJoiner.toString();
								 })
								 .collect(Collectors.joining("\n"));

		return Stream.concat(
				// Id column headers
				ResultTestUtil.ID_FIELDS.stream().map(i -> i.defaultColumnName(settings)),
				// result column headers
				getResultTypes().stream().map(ResultType::typeInfo)
		).collect(Collectors.joining("\t"))
			   + "\n" + expected + "\n";
	}

	private static String getPrintValue(Object obj, ResultType type, PrintSettings settings) {
		if (obj == null) {
			return "null";
		}
		if (type.equals(ResultType.DateRangeT.INSTANCE)) {
			// Special case for daterange in this test because it uses a StructVector, we rebuild the structural information
			List<?> dr = (List<?>) obj;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			final int min = (int) dr.get(0);
			final int max = (int) dr.get(1);
			// Handle cases where one of the limits is infinity
			if (!CDate.isNegativeInfinity(min)) {
				sb.append("\"min\":").append(min);
			}
			if (!CDate.isNegativeInfinity(min) && !CDate.isPositiveInfinity(max)) {
				sb.append(",");
			}
			if (!CDate.isPositiveInfinity(max)) {
				sb.append("\"max\":").append(max);
			}
			sb.append("}");
			return sb.toString();
		}
		if (type.equals(ResultType.ResolutionT.INSTANCE)) {
			return type.printNullable(settings, obj);
		}
		if (obj instanceof Collection) {
			Collection<?> col = (Collection<?>) obj;
			// Workaround: Arrow deserializes lists as a JsonStringArrayList which has a JSON String method
			new StringJoiner(",", "[", "]");
			@NonNull ResultType elemType = ((ResultType.ListT) type).getElementType();
			return col.stream().map(v -> getPrintValue(v, elemType, settings)).collect(Collectors.joining(", ", "[", "]"));
		}
		return obj.toString();
	}

	private static String getPrintValue(Object obj) {
		if (obj instanceof JsonStringArrayList) {
			// Workaround: Arrow deserializes lists as a JsonStringArrayList which has a JSON String method
			return getPrintValue(new ArrayList<>((JsonStringArrayList<?>) obj));
		}
		return Objects.toString(obj);
	}

}
