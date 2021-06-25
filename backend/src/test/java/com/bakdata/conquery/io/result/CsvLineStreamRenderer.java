package com.bakdata.conquery.io.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Result renderer for query tests.
 * In the future there might be a better format to check the query test results.
 */
@RequiredArgsConstructor
public class CsvLineStreamRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;

	public Stream<String> toStream(List<String> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream) {

		writer.addStringValues(idHeaders);
		infos.forEach(i -> writer.addValue(i.getUniqueName(cfg)));


		return Stream.concat(
				Stream.of(writer.writeValuesToString()),
				createCSVBody(cfg, infos, resultStream)
		);

	}

	private Stream<String> createCSVBody(PrintSettings cfg, List<ResultInfo> infos, Stream<EntityResult> results) {
		return results
				.map(result -> Pair.of(cfg.getIdMapper().map(result), result))
				.sorted(Comparator.comparing(Pair::getKey))
				.flatMap(res -> createCSVLine(cfg, infos, res));
	}


	private Stream<String> createCSVLine(PrintSettings cfg, List<ResultInfo> infos, Pair<ExternalEntityId, EntityResult> idResult) {
		return idResult
				.getValue()
				.streamValues()
				.map(result -> print(cfg, idResult.getKey(), infos, result));
	}

	private String print(PrintSettings cfg, ExternalEntityId entity, List<ResultInfo> infos, Object[] value) {
		List<String> result = new ArrayList<>(entity.getExternalId().length + value.length);
		result.addAll(Arrays.asList(entity.getExternalId()));
		try {
			for (int i = 0; i < infos.size(); i++) {
				result.add(infos.get(i).getType().printNullable(cfg, value[i]));
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value) + " with result infos " + infos, e);
		}

		return writer.writeRowToString(result);
	}
}
