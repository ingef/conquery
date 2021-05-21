package com.bakdata.conquery.io.result.csv;

import com.bakdata.conquery.io.result.ResultRender.ResultRenderProvider;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class CsvRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;



	public void toCSV(List<String> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream) {

		List<String> headers = new ArrayList<>(idHeaders);
		infos.forEach(i -> headers.add(i.getUniqueName(cfg)));

		writer.writeHeaders(headers);

		createCSVBody(cfg, infos, resultStream);
	}

	private void createCSVBody(PrintSettings cfg, List<ResultInfo> infos, Stream<EntityResult> results) {
		results
				.map(result -> Pair.of(cfg.getIdMapper().map(result), result))
				.sorted(Map.Entry.comparingByKey())
				.forEach(res -> res
								.getValue()
								.streamValues()
								.forEach(result -> printLine(cfg, res.getKey(), infos, result)));
	}


	public void printLine(PrintSettings cfg, ExternalEntityId entity, List<ResultInfo> infos, Object[] value) {
		// Cast here to Object[] so it is clear to intellij that the varargs call is intended
		writer.addValues((Object[]) entity.getExternalId());
		try {
			for (int i = 0; i < infos.size(); i++) {
				writer.addValue(infos.get(i).getType().printNullable(cfg, value[i]));
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value) + " with result infos " + infos, e);
		}

		writer.writeValuesToRow();
	}
}
