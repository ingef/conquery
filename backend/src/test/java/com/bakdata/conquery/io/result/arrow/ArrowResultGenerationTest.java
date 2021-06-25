package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultTestUtil.getResultTypes;
import static com.bakdata.conquery.io.result.ResultTestUtil.getTestEntityResults;
import static com.bakdata.conquery.io.result.arrow.ArrowRenderer.*;
import static com.bakdata.conquery.io.result.arrow.ArrowUtil.NAMED_FIELD_DATE_DAY;
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
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
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
    List<String> printIdFields = List.of("id1", "id2");

    @Test
    void generateFieldsIdMapping() {

        List<Field> fields = generateFieldsFromIdMapping(printIdFields);

        assertThat(fields).containsExactlyElementsOf(
                List.of(
                        new Field("id1", FieldType.nullable(new ArrowType.Utf8()), null),
                        new Field("id2", FieldType.nullable(new ArrowType.Utf8()), null)));

    }

    @Test
    void generateFieldsValue() {
        List<ResultInfo> resultInfos = getResultTypes().stream().map(ResultTestUtil.TypedSelectDummy::new)
                .map(select -> new SelectResultInfo(select, new CQConcept())).collect(Collectors.toList());

        List<Field> fields = generateFieldsFromResultType(
                resultInfos,
                // Custom column namer so we don't require a dataset registry
                new PrintSettings(false, Locale.ROOT, null, CONFIG, null,(selectInfo) -> selectInfo.getSelect().getLabel()));

        assertThat(fields).containsExactlyElementsOf(
                List.of(
                        new Field("BOOLEAN", FieldType.nullable(ArrowType.Bool.INSTANCE), null),
                        new Field("INTEGER", FieldType.nullable(new ArrowType.Int(32, true)), null),
                        new Field("NUMERIC", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
                        new Field("CATEGORICAL", FieldType.nullable(new ArrowType.Utf8()), null),
                        new Field("RESOLUTION", FieldType.nullable(new ArrowType.Utf8()), null),
                        new Field("DATE", FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null),
                        new Field("DATE_RANGE",
                                FieldType.nullable(ArrowType.Struct.INSTANCE),
                                List.of(
                                        NAMED_FIELD_DATE_DAY.apply("min"),
                                        NAMED_FIELD_DATE_DAY.apply("max")
                                )),
                        new Field("STRING", FieldType.nullable(new ArrowType.Utf8()), null),
                        new Field("MONEY", FieldType.nullable(new ArrowType.Int(32, true)), null),
                        new Field("LIST[BOOLEAN]", FieldType.nullable(ArrowType.List.INSTANCE), List.of(new Field("LIST[BOOLEAN]", FieldType.nullable(ArrowType.Bool.INSTANCE), null))),
                        new Field("LIST[DATE_RANGE]", FieldType.nullable(ArrowType.List.INSTANCE), List.of(new Field("LIST[DATE_RANGE]",
                                FieldType.nullable(ArrowType.Struct.INSTANCE),
                                List.of(
                                        NAMED_FIELD_DATE_DAY.apply("min"),
                                        NAMED_FIELD_DATE_DAY.apply("max")
                                ))))
                )
        );

    }

    @Test
    void writeAndRead() throws IOException {
        // Prepare every input data
        PrintSettings printSettings = new PrintSettings(
                false,
                Locale.ROOT,
                null,
                CONFIG,
                (cer) -> new ExternalEntityId(new String[]{Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())}),
                (selectInfo) -> selectInfo.getSelect().getLabel());
        // The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
        List<EntityResult> results = getTestEntityResults();

        ManagedQuery mquery = new ManagedQuery(null, null, null) {
            public List<ResultInfo> getResultInfo() {
                ResultInfoCollector coll = new ResultInfoCollector();
                coll.addAll(getResultTypes().stream()
                        .map(ResultTestUtil.TypedSelectDummy::new)
                        .map(select -> new SelectResultInfo(select, new CQConcept()))
                        .collect(Collectors.toList()));
                return coll.getInfos();
            }

			@Override
			public Stream<EntityResult> streamResults() {
				return results.stream();
			}
        };

        // First we write to the buffer, than we read from it and parse it as TSV
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        renderToStream((root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
                printSettings,
                BATCH_SIZE,
                printIdFields,
                mquery.getResultInfo(),
                mquery.streamResults());

        InputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        String computed = readTSV(inputStream);

        assertThat(computed).isNotBlank();
        assertThat(computed).isEqualTo(generateExpectedTSV(results, mquery.getResultInfo()));

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

    private String generateExpectedTSV(List<EntityResult> results, List<ResultInfo> resultInfos) {
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
                            valueJoiner.add(getPrintValue(val, info.getType()));
                        }
                        lineJoiner.add(valueJoiner.toString());
                    }
                    return lineJoiner.toString();
                })
                .collect(Collectors.joining("\n"));

        return printIdFields.stream().collect(Collectors.joining("\t")) + "\t" +
                getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining("\t")) + "\n" + expected + "\n";
    }

    private static String getPrintValue(Object obj, ResultType type) {
        if (obj != null && type.equals(ResultType.DateRangeT.INSTANCE)) {
            // Special case for daterange in this test because it uses a StructVector, we rebuild the structural information
            List dr = (List) obj;
            return "{\"min\":" + dr.get(0) + ",\"max\":" + dr.get(1) + "}";
        }
        if(obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            // Workaround: Arrow deserializes lists as a JsonStringArrayList which has a JSON String method
            new StringJoiner(",","[", "]");
            @NonNull ResultType elemType = ((ResultType.ListT) type).getElementType();
            return col.stream().map(v -> getPrintValue(v, elemType)).collect(Collectors.joining(", ","[", "]"));
        }
        return Objects.toString(obj);
    }

    private static String getPrintValue(Object obj) {
        if(obj instanceof JsonStringArrayList) {
            // Workaround: Arrow deserializes lists as a JsonStringArrayList which has a JSON String method
            return getPrintValue(new ArrayList<>((JsonStringArrayList)obj));
        }
        return Objects.toString(obj);
    }

}
