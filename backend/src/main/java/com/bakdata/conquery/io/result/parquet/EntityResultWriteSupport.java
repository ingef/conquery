package com.bakdata.conquery.io.result.parquet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.result.arrow.ArrowUtil;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
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
	private List<ColumnConsumer> columnConsumer;

	private RecordConsumer recordConsumer;

	@Override
	public WriteContext init(Configuration configuration) {
		schema = generateSchema(idHeaders, resultInfo, new UniqueNamer(printSettings));
		columnConsumer = generateColumnConsumers(idHeaders, resultInfo, printSettings);
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
		for (Object[] listResultLine : listResultLines) {
			recordConsumer.startMessage();
			// Write ID fields
			final String[] externalId = printSettings.getIdMapper().map(record).getExternalId();
			int cellIdx = 0;
			for (int i = 0; i < externalId.length; i++, cellIdx++) {
				final String subId = externalId[i];
				if (subId == null) {
					continue;
				}
				final String fieldName = schema.getFieldName(cellIdx);
				recordConsumer.startField(fieldName, cellIdx);
				columnConsumer.get(cellIdx).accept(recordConsumer, subId);
				recordConsumer.endField(fieldName, cellIdx);
			}

			// Write Result fields
			for (int i = 0; i < resultInfo.size(); i++, cellIdx++) {
				final Object resultValue = listResultLine[i];
				if (resultValue == null) {
					continue;
				}
				final String fieldName = schema.getFieldName(cellIdx);
				recordConsumer.startField(fieldName, cellIdx);
				columnConsumer.get(cellIdx).accept(recordConsumer, resultValue);
				recordConsumer.endField(fieldName, cellIdx);
			}

			recordConsumer.endMessage();
		}

	}

	/**
	 * Generates the parquet schema format from the {@link ResultInfo}s of a query
	 *
	 * @param idHeaders        {@link ResultInfo} for the Ids
	 * @param resultValueInfos {@link ResultInfo} for the result values
	 * @param uniqueNamer      A column namer for the fields in the schema
	 * @return the parquet schema
	 */
	public static MessageType generateSchema(
			List<ResultInfo> idHeaders,
			List<ResultInfo> resultValueInfos, UniqueNamer uniqueNamer) {

		/*
			Because Parquet Schemas rely on primitive types with logical annotations
			which are tedious to configure, we take the detour over the arrow schema.
		 */
		final SchemaMapping schemaMapping = new SchemaConverter().fromArrow(new Schema(ArrowUtil.generateFields(idHeaders, resultValueInfos, uniqueNamer)));

		return schemaMapping.getParquetSchema();

	}

	@Data
	private static class StringTColumnConsumer implements ColumnConsumer {

		private final ResultPrinters.Printer printer;
		private final PrintSettings printSettings;

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			final String printValue = getPrinter().print(o, printSettings);
			recordConsumer.addBinary(Binary.fromString(printValue));
		}
	}

	@RequiredArgsConstructor
	private static class BooleanTColumnConsumer implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addBoolean((boolean) o);
		}
	}

	@RequiredArgsConstructor
	private static class IntegerTColumnConsumer implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addInteger((int) o);
		}
	}

	@RequiredArgsConstructor
	private static class NumericTColumnConsumer implements ColumnConsumer {

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			recordConsumer.addDouble((double) o);
		}
	}

	@RequiredArgsConstructor
	private static class DateRangeTColumnConsumer implements ColumnConsumer {
		private final static String MIN_FIELD_NAME = "min";
		private final static String MAX_FIELD_NAME = "max";

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {
			List<Integer> dateRange = (List<Integer>) o;
			recordConsumer.startGroup();
			Integer min = dateRange.get(0);



			if (min != null && !(CDate.isNegativeInfinity(min))) {
				recordConsumer.startField(MIN_FIELD_NAME, 0);
				recordConsumer.addInteger(min);
				recordConsumer.endField(MIN_FIELD_NAME, 0);
			}

			Integer max = dateRange.get(1);
			if (max != null && !(CDate.isPositiveInfinity(max))) {
				recordConsumer.startField(MAX_FIELD_NAME, 1);
				recordConsumer.addInteger(max);
				recordConsumer.endField(MAX_FIELD_NAME, 1);
			}
			recordConsumer.endGroup();
		}
	}

	@RequiredArgsConstructor
	private static class ListTColumnConsumer implements ColumnConsumer {

		private final ColumnConsumer elementConsumer;
		private final PrintSettings printSettings;

		@Override
		public void accept(RecordConsumer recordConsumer, Object o) {

			List<?> list = (List<?>) o;

			if (list.isEmpty()) {
				// If the list is empty we still need to mark the field as an empty list: declare an empty group
				recordConsumer.startGroup();
				recordConsumer.endGroup();
				return;
			}

			// This nesting is wierd but documented https://github.com/apache/parquet-format/blob/master/LogicalTypes.md#lists
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

	private static List<ColumnConsumer> generateColumnConsumers(List<ResultInfo> idHeaders, List<ResultInfo> resultInfos, PrintSettings printSettings) {
		final List<ColumnConsumer> consumers = new ArrayList<>();
		for (ResultInfo idHeader : idHeaders) {
			consumers.add(getForResultType(idHeader.getType(), idHeader.getPrinter(), printSettings));
		}

		for (ResultInfo resultInfo : resultInfos) {
			consumers.add(getForResultType(resultInfo.getType(), resultInfo.getPrinter(), printSettings));
		}
		return consumers;
	}

	private static ColumnConsumer getForResultType(ResultType resultType, ResultPrinters.Printer printer, PrintSettings printSettings) {

		if (resultType instanceof ResultType.ListT<?> listT) {
			return new ListTColumnConsumer(getForResultType(listT.getElementType(), ((ResultPrinters.ListPrinter) printer).elementPrinter(), printSettings), printSettings);
		}

		return switch (((ResultType.Primitive) resultType)) {
			case BOOLEAN -> new BooleanTColumnConsumer();
			case INTEGER, DATE, MONEY -> new IntegerTColumnConsumer();
			case NUMERIC -> new NumericTColumnConsumer();
			case DATE_RANGE -> new DateRangeTColumnConsumer();
			case STRING -> new StringTColumnConsumer(printer, printSettings);
		};
	}
}
