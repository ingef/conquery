package com.bakdata.conquery.io.result.parquet;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.bakdata.conquery.io.result.arrow.ArrowUtil;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.ArrowResultPrinters;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.arrow.schema.SchemaConverter;
import org.apache.parquet.arrow.schema.SchemaMapping;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.jetbrains.annotations.NotNull;

/**
 * {@link WriteSupport} for Conquery's {@link EntityResult} type.
 * Each ResultType is covered by a {@link ColumnConsumer} which is aligned
 * with the schema.
 */
@Slf4j
@RequiredArgsConstructor
public class EntityResultWriteSupport extends WriteSupport<EntityResult> {


	private final List<ResultInfo> idHeaders;
	private final List<ResultInfo> resultInfo;
	private final PrintSettings printSettings;

	private MessageType schema;
	private List<ColumnConsumer> columnConsumers;
	private List<Printer> columnPrinters;

	private RecordConsumer recordConsumer;

	/**
	 * Generates the parquet schema format from the {@link ResultInfo}s of a query
	 *
	 * @param idHeaders        {@link ResultInfo} for the Ids
	 * @param resultValueInfos {@link ResultInfo} for the result values
	 * @param uniqueNamer      A column namer for the fields in the schema
	 * @param printSettings1
	 * @return the parquet schema
	 */
	public static MessageType generateSchema(List<ResultInfo> idHeaders, List<ResultInfo> resultValueInfos, UniqueNamer uniqueNamer, PrintSettings printSettings1) {
		/*
			Because Parquet Schemas rely on primitive types with logical annotations
			which are tedious to configure, we take the detour over the arrow schema.
		 */
		final SchemaMapping schemaMapping =
				new SchemaConverter().fromArrow(new Schema(ArrowUtil.generateFields(idHeaders, resultValueInfos, uniqueNamer, printSettings1)));

		return schemaMapping.getParquetSchema();

	}

	private static List<ColumnConsumer> generateColumnConsumers(List<ResultInfo> idHeaders, List<ResultInfo> resultInfos) {
		return Stream.concat(idHeaders.stream(), resultInfos.stream())
					 .map(ResultInfo::getType)
					 .map(EntityResultWriteSupport::columnConsumerForType)
					 .toList();

	}

	private static List<Printer> generateColumnPrinters(List<ResultInfo> idHeaders, List<ResultInfo> resultInfos, PrintSettings printSettings, PrinterFactory printerFactory) {

		return Stream.concat(idHeaders.stream(), resultInfos.stream())
					 .map(info -> info.createPrinter(printerFactory, printSettings))
					 .toList();

	}

	private static ColumnConsumer columnConsumerForType(ResultType resultType) {

		if (resultType instanceof ResultType.ListT<?> listT) {
			return new ListColumnConsumer(columnConsumerForType(listT.getElementType()));
		}

		return switch (((ResultType.Primitive) resultType)) {
			case BOOLEAN -> new BooleanColumnConsumer();
			case INTEGER, DATE -> new IntegerColumnConsumer();
			case NUMERIC -> new NumericColumnConsumer();
			case MONEY -> new MoneyColumnConsumer();
			case DATE_RANGE -> new DateRangeColumnConsumer();
			case STRING -> new StringColumnConsumer();
		};
	}

	@Override
	public WriteContext init(Configuration configuration) {
		schema = generateSchema(idHeaders, resultInfo, new UniqueNamer(printSettings), printSettings);
		columnConsumers = generateColumnConsumers(idHeaders, resultInfo);
		columnPrinters = generateColumnPrinters(idHeaders, resultInfo, printSettings, new ArrowResultPrinters());
		return new WriteContext(schema, Map.of());
	}

	@Override
	public void prepareForWrite(RecordConsumer recordConsumer) {
		this.recordConsumer = recordConsumer;
	}

	@Override
	public void write(EntityResult record) {
		final List<Object[]> listResultLines = record.listResultLines();

		if (record instanceof MultilineEntityResult) {
			// Warn if we still encounter a MultilineEntityResult.
			// This should not happen because of the workaround in ParquetRenderer
			log.warn("Processing a MultilineEntityResult is not working properly. Only the first line will be output");
		}

		// Write ID fields
		final Object[] printedExternalId = getPrintedExternalId(record);

		for (Object[] listResultLine : listResultLines) {
			recordConsumer.startMessage();

			for (int index = 0; index < printedExternalId.length; index++) {
				final Object printed = printedExternalId[index];
				if (printed == null) {
					continue;
				}

				final String fieldName = schema.getFieldName(index);

				recordConsumer.startField(fieldName, index);
				columnConsumers.get(index).accept(recordConsumer, printed);
				recordConsumer.endField(fieldName, index);
			}

			// Write Result fields
			for (int index = 0; index < listResultLine.length; index++) {
				final int colId = index + printedExternalId.length;

				final Object value = listResultLine[index];

				if (value == null) {
					// Parquet consumers cannot handle null?
					continue;
				}

				Printer printer = columnPrinters.get(colId);
				final Object printed = printer.apply(value);

				final String fieldName = schema.getFieldName(colId);

				recordConsumer.startField(fieldName, colId);
				columnConsumers.get(colId).accept(recordConsumer, printed);
				recordConsumer.endField(fieldName, colId);
			}

			recordConsumer.endMessage();
		}

	}

	@NotNull
	private Object[] getPrintedExternalId(EntityResult record) {
		final String[] externalId = printSettings.getIdMapper().map(record).getExternalId();

		final Object[] printedExternalId = new String[externalId.length];

		for (int index = 0; index < externalId.length; index++) {
			Printer printer = columnPrinters.get(index);
			printedExternalId[index] = printer.apply(externalId[index]);
		}
		return printedExternalId;
	}

	private record StringColumnConsumer() implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addBinary(Binary.fromString((String) o));
		}
	}

	private record BooleanColumnConsumer() implements ColumnConsumer {
		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addBoolean((boolean) o);
		}
	}

	private record IntegerColumnConsumer() implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addInteger(((Number) o).intValue());
		}
	}


	private record NumericColumnConsumer() implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addDouble(((Number) o).doubleValue());
		}
	}

	private record MoneyColumnConsumer() implements ColumnConsumer {
		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addInteger(((Integer) o));
		}
	}


	private record DateRangeColumnConsumer() implements ColumnConsumer {
		private static final String MIN_FIELD_NAME = "min";
		private static final String MAX_FIELD_NAME = "max";

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			final CDateRange dateRange = (CDateRange) o;

			recordConsumer.startGroup();

			if (dateRange.hasLowerBound()) {
				recordConsumer.startField(MIN_FIELD_NAME, 0);
				recordConsumer.addInteger(dateRange.getMinValue());
				recordConsumer.endField(MIN_FIELD_NAME, 0);
			}

			if (dateRange.hasUpperBound()) {
				recordConsumer.startField(MAX_FIELD_NAME, 1);
				recordConsumer.addInteger(dateRange.getMaxValue());
				recordConsumer.endField(MAX_FIELD_NAME, 1);
			}

			recordConsumer.endGroup();
		}
	}

	private record ListColumnConsumer(ColumnConsumer elementConsumer) implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {

			List<?> list = (List<?>) o;

			if (list.isEmpty()) {
				// If the list is empty we still need to mark the field as an empty list: declare an empty group
				recordConsumer.startGroup();
				recordConsumer.endGroup();
				return;
			}

			// This nesting is weird but documented https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists
			recordConsumer.startGroup();
			recordConsumer.startField("list", 0);
			for (Object elem : list) {
				recordConsumer.startGroup();
				recordConsumer.startField("list", 0);
				elementConsumer.accept(recordConsumer, elem);
				recordConsumer.endField("list", 0);
				recordConsumer.endGroup();
			}
			recordConsumer.endField("list", 0);
			recordConsumer.endGroup();
		}
	}
}
