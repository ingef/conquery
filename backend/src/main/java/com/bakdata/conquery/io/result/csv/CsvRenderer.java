package com.bakdata.conquery.io.result.csv;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.io.result.PreparedResultColumn;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.StringResultPrinters;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CsvRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;

	public void toCSV(List<ResultInfo> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream, Charset charset) {
		final PreparedResultColumn.Columns columns = PreparedResultColumn.prepare(
				idHeaders,
				infos,
				cfg,
				StringResultPrinters.forCharset(charset)
		);
		final String[] headers = columns.allColumns().stream().map(PreparedResultColumn::name).toArray(String[]::new);

		writer.writeHeaders(headers);

		createCSVBody(cfg, columns.resultColumns(), resultStream);
	}

	private void createCSVBody(
			PrintSettings cfg, List<PreparedResultColumn> columns, Stream<EntityResult> results) {
		final Printer[] printers = columns.stream().map(PreparedResultColumn::printer).toArray(Printer[]::new);

		results.forEach(result -> {
			final EntityPrintId entityPrintId = cfg.getIdMapper().map(result);
			result.streamValues().forEach(values -> printLine(entityPrintId, printers, values));
		});
	}


	public void printLine(EntityPrintId entity, Printer[] printers, Object[] values) {
		// Cast here to Object[] so it is clear to intellij that the varargs call is intended
		writer.addValues((Object[]) entity.getExternalId());
		for (int i = 0; i < printers.length; i++) {
			final Object value = values[i];

			if (value == null) {
				writer.addValue("");
				continue;
			}
			try {
				writer.addValue(printers[i].apply(value));
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to print column %s of line %s".formatted(i, Arrays.deepToString(values)), e);
			}
		}

		writer.writeValuesToRow();
	}
}
