package com.bakdata.conquery.io.result.csv;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@RequiredArgsConstructor
@Slf4j
public class CsvRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;

	public void toCSV(List<ResultInfo> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream, PrintSettings printSettings) {

		UniqueNamer uniqNamer = new UniqueNamer(cfg);
		final String[] headers = Stream.concat(idHeaders.stream(), infos.stream()).map(info -> uniqNamer.getUniqueName(info, printSettings)).toArray(String[]::new);

		writer.writeHeaders(headers);

		createCSVBody(cfg, infos, resultStream, printSettings);
	}

	private void createCSVBody(PrintSettings cfg, List<ResultInfo> infos, Stream<EntityResult> results, PrintSettings printSettings) {
		final Printer[] printers = infos.stream().map(info -> info.createPrinter(printSettings)).toArray(Printer[]::new);

		results.map(result -> Pair.of(cfg.getIdMapper().map(result), result))
			   .sorted(Map.Entry.comparingByKey())
			   .forEach(res -> res
					   .getValue()
					   .streamValues()
					   .forEach(result -> printLine(res.getKey(), printers, result)));
	}


	public void printLine(EntityPrintId entity, Printer[] printers, Object[] values) {
		// Cast here to Object[] so it is clear to intellij that the varargs call is intended
		writer.addValues((Object[]) entity.getExternalId());
		try {
			for (int i = 0; i < printers.length; i++) {
				final Object value = values[i];

				if (value == null) {
					writer.addValue("");
					continue;
				}

				writer.addValue(printers[i].apply(value));
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(values), e);
		}

		writer.writeValuesToRow();
	}
}
