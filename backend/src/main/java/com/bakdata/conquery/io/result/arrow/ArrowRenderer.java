package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ArrowUtil.ROOT_ALLOCATOR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ArrowConfig;
import com.bakdata.conquery.models.identifiable.mapping.PrintIdMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
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
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.Text;

@Slf4j
public class ArrowRenderer {

	public static void renderToStream(
			Function<VectorSchemaRoot, ArrowWriter> writerProducer,
			PrintSettings printSettings,
			ArrowConfig arrowConfig,
			List<ResultInfo> idHeaders,
			List<ResultInfo> resultInfo,
			Stream<EntityResult> results) throws IOException {

		final List<Field> fields = ArrowUtil.generateFields(idHeaders, resultInfo, new UniqueNamer(printSettings));
		final VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, null), ROOT_ALLOCATOR);

		// Build separate pipelines for id and value, as they have different sources but the same target
		final RowConsumer[] idWriters = generateWriterPipeline(root, 0, idHeaders.size(), idHeaders);
		final RowConsumer[] valueWriter = generateWriterPipeline(root, idHeaders.size(), resultInfo.size(), resultInfo);

		final List<Printer> printers = new ArrayList<>();

		for (ResultInfo header : idHeaders) {
			printers.add(header.getPrinter());
		}

		for (ResultInfo info : resultInfo) {
			printers.add(info.getPrinter());
		}


		// Write the data
		try (ArrowWriter writer = writerProducer.apply(root)) {
			write(writer, root, idWriters, valueWriter, printSettings.getIdMapper(), printers, results, arrowConfig.getBatchSize());
		}

	}


	public static void write(
			ArrowWriter writer,
			VectorSchemaRoot root,
			RowConsumer[] idWriters,
			RowConsumer[] valueWriters,
			PrintIdMapper idMapper,
			List<Printer> printers,
			Stream<EntityResult> results,
			int batchSize) throws IOException {
		Preconditions.checkArgument(batchSize > 0, "Batch size needs be larger than 0.");
		// TODO add time metric for writing

		log.trace("Starting result write");

		writer.start();
		int batchCount = 0;
		int batchLineCount = 0;
		final Iterator<EntityResult> resultIterator = results.iterator();
		while (resultIterator.hasNext()) {
			final EntityResult cer = resultIterator.next();

			final String[] externalId = idMapper.map(cer).getExternalId();

			final Object[] printedExternalId = new String[externalId.length];

			for (int index = 0; index < idWriters.length; index++) {
				printedExternalId[index] = printers.get(index).print(externalId[index]);
			}

			for (Object[] line : cer.listResultLines()) {
				Preconditions.checkState(
						line.length == valueWriters.length,
						"The number of value writers and values in a result line differs. Writers: %d Line: %d".formatted(valueWriters.length, line.length)
				);

				for (int index = 0; index < idWriters.length; index++) {
					if(printedExternalId[index] == null){
						continue;
					}

					idWriters[index].accept(batchLineCount, printedExternalId[index]);
				}

				for (int index = 0; index < valueWriters.length; index++) {
					final int colId = index + idWriters.length;
					// In this case, the printer normalizes and adjusts values.

					final Object printed = printers.get(colId).print(line[index]);

					if (printed == null) {
						continue;
					}
					valueWriters[index].accept(batchLineCount, printed);
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

	private static RowConsumer intVectorFiller(IntVector vector) {
		return (rowNumber, valueRaw) -> {
			final Integer value = (Integer) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, value);
		};
	}

	private static RowConsumer bitVectorFiller(BitVector vector) {
		return (rowNumber, valueRaw) -> {
			final Boolean value = (Boolean) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, value ? 1 : 0);
		};
	}

	private static RowConsumer float8VectorFiller(Float8Vector vector) {
		return (rowNumber, valueRaw) -> {
			final Number value = (Number) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, value.doubleValue());
		};
	}

	private static RowConsumer float4VectorFiller(Float4Vector vector) {
		return (rowNumber, valueRaw) -> {
			final Number value = (Number) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, value.floatValue());
		};
	}

	private static RowConsumer varCharVectorFiller(VarCharVector vector) {
		return (rowNumber, valueRaw) -> {
			final String value = (String) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}
			vector.setSafe(rowNumber, new Text(value));
		};
	}

	private static RowConsumer dateDayVectorFiller(DateDayVector vector) {
		return (rowNumber, valueRaw) -> {
			final Number value = (Number) valueRaw;
			if (value == null) {
				vector.setNull(rowNumber);
				return;
			}

			// Treat our internal infinity dates (Interger.MIN and Integer.MAX) also as null
			final int epochDay = value.intValue();

			if (CDate.isNegativeInfinity(epochDay) || CDate.isPositiveInfinity(epochDay)) {
				vector.setNull(rowNumber);
				return;
			}

			vector.setSafe(rowNumber, epochDay);
		};
	}

	private static RowConsumer structVectorFiller(StructVector vector, RowConsumer[] nestedConsumers) {
		return (rowNumber, valueRaw) -> {
			// Values is a horizontal list
			final List<?> values = (List<?>) valueRaw;
			if (values == null) {
				vector.setNull(rowNumber);
				return;
			}

			Preconditions.checkState(
					values.size() == nestedConsumers.length,
					"The number of the provided nested value differs from the number of consumer for the generated vectors. Provided values: %s\t Available consumers: %d".formatted(values, nestedConsumers.length));

			for (RowConsumer nestedConsumer : nestedConsumers) {
				nestedConsumer.accept(rowNumber, values.toArray());
			}

			// Finally mark that we populated the nested vectors
			vector.setIndexDefined(rowNumber);
		};
	}

	private static RowConsumer listVectorFiller(ListVector vector, RowConsumer nestedConsumer) {
		return (rowNumber, valueRaw) -> {
			// Values is a vertical list
			final List<?> values = (List<?>) valueRaw;

			if (values == null) {
				vector.setNull(rowNumber);
				return;
			}

			final int start = vector.startNewValue(rowNumber);

			for (int i = 0; i < values.size(); i++) {
				// These short lived one value arrays are a workaround at the moment
				nestedConsumer.accept(Math.addExact(start, i), new Object[]{values.get(i)});
			}

			vector.endValue(rowNumber, values.size());
		};
	}


	public static RowConsumer[] generateWriterPipeline(VectorSchemaRoot root, int vectorOffset, int numVectors, List<ResultInfo> resultInfos) {
		Preconditions.checkArgument(vectorOffset >= 0, "Offset was negative: %s", vectorOffset);
		Preconditions.checkArgument(numVectors >= 0, "Number of vectors was negative: %s", numVectors);

		final RowConsumer[] builder = new RowConsumer[numVectors];

		for (int vecI = vectorOffset; (vecI < root.getFieldVectors().size()) && (vecI < vectorOffset + numVectors); vecI++) {
			final int pos = vecI - vectorOffset;
			final FieldVector vector = root.getVector(vecI);
			final ResultInfo resultInfo = resultInfos.get(pos);
			builder[pos] = generateVectorFiller(vector, resultInfo.getType());

		}
		return builder;

	}

	private static RowConsumer generateVectorFiller(ValueVector vector, ResultType type) {
		if (type instanceof ResultType.ListT<?> listT) {
			final ValueVector nestedVector = ((ListVector) vector).getDataVector();

			return listVectorFiller(((ListVector) vector), generateVectorFiller(nestedVector, listT.getElementType()));
		}
		//TODO who dis?
		//		if (vector instanceof Float4Vector float4Vector) {
		//			return float4VectorFiller(float4Vector, (line) -> (Number) line[pos]);
		//		}

		return switch (((ResultType.Primitive) type)) {
			case BOOLEAN -> bitVectorFiller(((BitVector) vector));
			case INTEGER -> intVectorFiller(((IntVector) vector));
			case MONEY -> float8VectorFiller(((Float8Vector) vector));
			case DATE -> dateDayVectorFiller(((DateDayVector) vector));
			case NUMERIC -> float8VectorFiller((Float8Vector) vector);
			case STRING -> varCharVectorFiller(((VarCharVector) vector));

			case DATE_RANGE -> {
				final StructVector structVector = (StructVector) vector;
				final List<ValueVector> nestedVectors = structVector.getPrimitiveVectors();
				final RowConsumer[] nestedConsumers = new RowConsumer[nestedVectors.size()];

				for (int i = 0; i < nestedVectors.size(); i++) {
					nestedConsumers[i] = generateVectorFiller(nestedVectors.get(i), ResultType.Primitive.DATE);
				}

				yield structVectorFiller(structVector, nestedConsumers);
			}

		};


	}

}
