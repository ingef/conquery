package com.bakdata.conquery.io.result.csv;

import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
				.sorted(Comparator.comparing(Pair::getKey))
				.forEach(res -> createCSVLine(cfg, infos, res));
	}


	private void createCSVLine(PrintSettings cfg, List<ResultInfo> infos, Pair<ExternalEntityId, EntityResult> idResult) {
		idResult
				.getValue()
				.streamValues()
				.forEach(result -> print(cfg, idResult.getKey(), infos, result));
	}

	public void print(PrintSettings cfg, ExternalEntityId entity, List<ResultInfo> infos, Object[] value) {
		List<String> result = new ArrayList<>(entity.getExternalId().length + value.length);
		result.addAll(Arrays.asList(entity.getExternalId()));
		try {
			for (int i = 0; i < infos.size(); i++) {
				result.add(infos.get(i).getType().printNullable(cfg, value[i]));
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value) + " with result infos " + infos, e);
		}

		writer.writeRow(result);
	}
}
