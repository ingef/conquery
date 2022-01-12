package com.bakdata.conquery.io.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
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

	public Stream<String> toStream(List<ResultInfo> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream) {

		final List<ResultInfo> allInfos = new ArrayList<>(idHeaders);
		allInfos.addAll(infos);

		UniqueNamer uniqNamer = new UniqueNamer(cfg, allInfos);
		Stream.concat(idHeaders.stream(), infos.stream()).map(uniqNamer::getUniqueName).forEach(writer::addValue);


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


	private Stream<String> createCSVLine(PrintSettings cfg, List<ResultInfo> infos, Pair<EntityPrintId, EntityResult> idResult) {
		return idResult
				.getValue()
				.streamValues()
				.map(result -> print(cfg, idResult.getKey(), infos, result));
	}

	private String print(PrintSettings cfg, EntityPrintId entity, List<ResultInfo> infos, Object[] value) {
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
