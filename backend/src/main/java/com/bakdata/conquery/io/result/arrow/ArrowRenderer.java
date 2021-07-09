package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
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
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.complex.BaseListVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.complex.impl.UnionListWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;
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
            int batchSize,
            List<String> idHeaders,
            List<ResultInfo> resultInfo,
            Stream<EntityResult> results) throws IOException {

        // Combine id and value Fields to one vector to build a schema
        final List<Field> idFields = generateFieldsFromIdMapping(idHeaders);
        List<Field> fields = new ArrayList<>(idFields);
        fields.addAll(generateFieldsFromResultType(resultInfo, cfg));
        VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, null), ROOT_ALLOCATOR);

        // Build separate pipelines for id and value, as they have different sources but the same target
        RowConsumer[] idWriters = generateWriterPipeline(root, 0, idHeaders.size());
        RowConsumer[] valueWriter = generateWriterPipeline(root, idHeaders.size(), resultInfo.size());

        // Write the data
        try (ArrowWriter writer = writerProducer.apply(root)) {
            write(writer, root, idWriters, valueWriter, cfg.getIdMapper(), results, batchSize);
        }

    }


    public static void write(
            ArrowWriter writer,
            VectorSchemaRoot root,
            RowConsumer[] idWriter,
            RowConsumer[] valueWriter,
            PrintIdMapper idMapper,
            Stream<EntityResult> results,
            int batchSize) throws IOException {
        Preconditions.checkArgument(batchSize > 0, "Batch size needs be larger than 0.");
        // TODO add time metric for writing

        log.trace("Starting result write");
        writer.start();
        int batchCount = 0;
        int batchLineCount = 0;
        Iterator<EntityResult> resultIterator = results.iterator();
        while (resultIterator.hasNext()) {
            EntityResult cer = resultIterator.next();
            for (Object[] line : cer.listResultLines()) {
                if(line.length != valueWriter.length) {
                    throw new IllegalStateException("The number of value writers and values in a result line differs. Writers: " + valueWriter.length + " Line: " + line.length);
                }
                for (RowConsumer rowConsumer : idWriter) {
                    // Write id information
                    rowConsumer.accept(batchLineCount, idMapper.map(cer).getExternalId());
                }
                for (RowConsumer rowConsumer : valueWriter) {
                    // Write values
                    rowConsumer.accept(batchLineCount, line);
                }
                batchLineCount++;

                if (batchLineCount >= batchSize) {
                    root.setRowCount(batchLineCount);
                    writer.writeBatch();
                    root.clear();
                    batchLineCount = 0;
                }
            }
        }
        if (batchLineCount > 0) {
            root.setRowCount(batchLineCount);
            writer.writeBatch();
            root.clear();
            batchCount++;
        }
        log.trace("Wrote {} batches of size {} (last batch might be smaller)", batchCount, batchSize);
        writer.end();
    }

    private static RowConsumer intVectorFiller(IntVector vector, Function<Object[], Integer> resultExtractor) {
        return (rowNumber, line) -> {
            Integer value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, value);
        };
    }

    private static RowConsumer bitVectorFiller(BitVector vector, Function<Object[], Boolean> resultExtractor) {
        return (rowNumber, line) -> {
            Boolean value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, value ? 1 : 0);
        };
    }

    private static RowConsumer float8VectorFiller(Float8Vector vector, Function<Object[], Number> resultExtractor) {
        return (rowNumber, line) -> {
            Number value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, value.doubleValue());
        };
    }

    private static RowConsumer float4VectorFiller(Float4Vector vector, Function<Object[], Number> resultExtractor) {
        return (rowNumber, line) -> {
            Number value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, value.floatValue());
        };
    }

    private static RowConsumer varCharVectorFiller(VarCharVector vector, Function<Object[], String> resultExtractor) {
        return (rowNumber, line) -> {
            String value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, new Text(value));
        };
    }

    private static RowConsumer dateDayVectorFiller(DateDayVector vector, Function<Object[], Number> resultExtractor) {
        return (rowNumber, line) -> {
            Number value = resultExtractor.apply(line);
            if (value == null) {
                vector.setNull(rowNumber);
                return;
            }
            vector.setSafe(rowNumber, value.intValue());
        };
    }

    private static RowConsumer structVectorFiller(StructVector vector, RowConsumer [] nestedConsumers,  Function<Object[], List<?>> resultExtractor) {
        return (rowNumber, line) -> {
            // Values is a horizontal list
            List<?> values = resultExtractor.apply(line);
            if (values == null) {
                vector.setNull(rowNumber);
                return;
            }
            if(values.size() != nestedConsumers.length) {
                throw new IllegalStateException("The number of the provided nested value differs from the number of consumer for the generated vectors. Provided values: " + values + "\t Available consumers: " + nestedConsumers.length);
            }
            for (RowConsumer nestedConsumer : nestedConsumers) {
                nestedConsumer.accept(rowNumber, values.toArray());
            }

            // Finally mark that we populated the nested vectors
            vector.setIndexDefined(rowNumber);
        };
    }

    private static RowConsumer listVectorFiller(ListVector vector, RowConsumer nestedConsumer, Function<Object[], List<?>> resultExtractor){
        return (rowNumber, line) -> {
            // Values is a vertical list
            List<?> values = resultExtractor.apply(line);
            if (values == null) {
                vector.setNull(rowNumber);
                return;
            }

            int start = vector.startNewValue(rowNumber);

            for (int i = 0; i < values.size(); i++) {
                // These short lived one value arrays are a workaround at the moment
                nestedConsumer.accept(Math.addExact(start, i), new Object[] {values.get(i)});
            }

            vector.endValue(rowNumber, values.size());
       };
    }


    public static RowConsumer[] generateWriterPipeline(VectorSchemaRoot root, int vectorOffset, int numVectors) {
        Preconditions.checkArgument(vectorOffset >= 0, "Offset was negative: %s", vectorOffset);
        Preconditions.checkArgument(numVectors >= 0, "Number of vectors was negative: %s", numVectors);

        RowConsumer[] builder = new RowConsumer[numVectors];

        for (
                int vecI = vectorOffset;
                (vecI < root.getFieldVectors().size()) && (vecI < vectorOffset + numVectors);
                vecI++
        ) {
            final int pos = vecI - vectorOffset;
            final FieldVector vector = root.getVector(vecI);

            builder[pos] = generateVectorFiller(pos, vector);

        }
        return builder;

    }

    private static RowConsumer generateVectorFiller(int pos, ValueVector vector) {
        //TODO When Pattern-matching lands, clean this up. (Think Java 12?)
        if (vector instanceof IntVector) {
            return intVectorFiller((IntVector) vector, (line) -> (Integer) line[pos]);
        }

        if (vector instanceof VarCharVector) {
            return varCharVectorFiller((VarCharVector) vector, (line) -> (line[pos] instanceof String) ? (String) line[pos] : (line[pos] != null ? Objects.toString(line[pos]) : null));
        }

        if (vector instanceof BitVector) {
            return bitVectorFiller((BitVector) vector, (line) -> (Boolean) line[pos]);
        }

        if (vector instanceof Float4Vector) {
            return float4VectorFiller((Float4Vector) vector, (line) -> (Number) line[pos]);
        }

        if (vector instanceof Float8Vector) {
            return float8VectorFiller((Float8Vector) vector, (line) -> (Number) line[pos]);
        }

        if (vector instanceof DateDayVector) {
            return dateDayVectorFiller((DateDayVector) vector, (line) -> (Number) line[pos]);
        }

        if (vector instanceof StructVector) {
            StructVector structVector = (StructVector) vector;

            List<ValueVector> nestedVectors = structVector.getPrimitiveVectors();
            RowConsumer [] nestedConsumers = new RowConsumer[nestedVectors.size()];
            for (int i = 0; i < nestedVectors.size(); i++) {
                nestedConsumers[i] = generateVectorFiller(i, nestedVectors.get(i));
            }
            return structVectorFiller(structVector, nestedConsumers, (line) -> (List<?>) line[pos]);
        }

        if (vector instanceof ListVector) {
            ListVector listVector = (ListVector) vector;

            ValueVector nestedVector = listVector.getDataVector();

            // pos = 0 is a workaround for now
            return listVectorFiller(listVector, generateVectorFiller(0, nestedVector), (line) -> (List<?>) line[pos]);
        }

        throw new IllegalArgumentException("Unsupported vector type " + vector);
    }

    public static List<Field> generateFieldsFromIdMapping(List<String> idHeaders) {
        Preconditions.checkArgument(idHeaders != null && idHeaders.size() > 0, "No id headers given");

        ImmutableList.Builder<Field> fields = ImmutableList.builder();

        for (String header : idHeaders) {
            fields.add(new Field(header, FieldType.nullable(new ArrowType.Utf8()), null));
        }

        return fields.build();
    }

    public static List<Field> generateFieldsFromResultType(@NonNull List<ResultInfo> info, PrintSettings settings) {
        return info.stream()
                .map(i -> i.getType().getArrowFieldType(i, settings))
                .collect(Collectors.toUnmodifiableList());

    }

}
