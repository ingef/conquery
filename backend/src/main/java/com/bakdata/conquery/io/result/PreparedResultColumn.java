package com.bakdata.conquery.io.result;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;

public record PreparedResultColumn(String name, ResultInfo resultInfo, ResultType type, Printer printer) {

	public static Columns prepare(
			List<ResultInfo> idResultInfos,
			List<ResultInfo> resultInfos,
			PrintSettings printSettings,
			PrinterFactory printerFactory) {
		return prepare(idResultInfos, resultInfos, new UniqueNamer(printSettings), printSettings, printerFactory);
	}

	public static Columns prepare(
			List<ResultInfo> idResultInfos,
			List<ResultInfo> resultInfos,
			UniqueNamer uniqueNamer,
			PrintSettings printSettings,
			PrinterFactory printerFactory) {
		return new Columns(
				prepare(idResultInfos, uniqueNamer, printSettings, printerFactory),
				prepare(resultInfos, uniqueNamer, printSettings, printerFactory)
		);
	}

	private static List<PreparedResultColumn> prepare(
			List<ResultInfo> resultInfos,
			UniqueNamer uniqueNamer,
			PrintSettings printSettings,
			PrinterFactory printerFactory) {
		return resultInfos.stream()
					  .map(info -> new PreparedResultColumn(
							  uniqueNamer.getUniqueName(info, printSettings),
							  info,
							  info.getType(),
							  info.createPrinter(printerFactory, printSettings)
					  ))
					  .toList();
	}

	public record Columns(List<PreparedResultColumn> idColumns, List<PreparedResultColumn> resultColumns) {

		public Columns {
			idColumns = List.copyOf(idColumns);
			resultColumns = List.copyOf(resultColumns);
		}

		public List<PreparedResultColumn> allColumns() {
			List<PreparedResultColumn> columns = new ArrayList<>(idColumns.size() + resultColumns.size());
			columns.addAll(idColumns);
			columns.addAll(resultColumns);
			return columns;
		}
	}
}
