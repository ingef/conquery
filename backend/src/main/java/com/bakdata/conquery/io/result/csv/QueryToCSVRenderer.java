package com.bakdata.conquery.io.result.csv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.univocity.parsers.csv.CsvWriter;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class QueryToCSVRenderer {

	private static final IdMappingConfig ID_MAPPING = ConqueryConfig.getInstance().getIdMapping();
	private static final Collection<String> HEADER = Arrays.asList(ID_MAPPING.getPrintIdFields());
	
	public static Stream<String> toCSV(PrintSettings cfg, ManagedQuery query, Function<ContainedEntityResult,ExternalEntityId> idMapper) {
		return toCSV(cfg, List.of(query), idMapper);
	}
	
	public static Stream<String> toCSV(PrintSettings cfg, Collection<ManagedQuery> queries, Function<ContainedEntityResult,ExternalEntityId> idMapper) {
		if (queries.stream()
			.anyMatch(q -> q.getState() != ExecutionState.DONE)) {
			throw new IllegalArgumentException("Can only create a CSV from a successfully finished Query " + queries.iterator().next().getId());
		}

		ResultInfoCollector infos = queries.iterator().next().collectResultInfos();
		
		//build header
		CsvWriter writer = CsvIo.createWriter();
		writer.addStringValues(HEADER);
		for(ResultInfo info : infos.getInfos()) {
			writer.addValue(info.getUniqueName(cfg));
		}
		
		return Stream.concat(
			Stream.of(writer.writeValuesToString()),
			queries
				.stream()
				.flatMap(
					q -> createCSVBody(
						writer,
						cfg,
						q.collectResultInfos(),
						q,
						idMapper))
		);
	}

	private static Stream<String> createCSVBody(CsvWriter writer, PrintSettings cfg, ResultInfoCollector infos, ManagedQuery query, Function<ContainedEntityResult,ExternalEntityId> idMapper) {
		Namespace namespace = Objects.requireNonNull(query.getNamespace());
		return query.getResults()
			.stream()
			.flatMap(ContainedEntityResult::filterCast)
			.map(result -> Pair.of(idMapper.apply(result), result))
			.sorted(Comparator.comparing(Pair::getKey))
			.flatMap(res -> createCSVLine(writer, cfg, infos, res));
	}

	
	private static Stream<String> createCSVLine(CsvWriter writer, PrintSettings cfg, ResultInfoCollector infos, Pair<ExternalEntityId, ContainedEntityResult> idResult) {
		return idResult
			.getValue()
			.streamValues()
			.map(result -> print(writer, cfg, idResult.getKey(), infos, result));
	}
	
	public static String print(CsvWriter writer, PrintSettings cfg, ExternalEntityId entity, ResultInfoCollector infos, Object[] value) {
		List<String> result = new ArrayList<>(entity.getExternalId().length + value.length);
		result.addAll(Arrays.asList(entity.getExternalId()));
		try {
			for(int i=0;i<infos.size();i++) {
				result.add(infos.getInfos().get(i).getType().printNullable(cfg, value[i]));
			}
		}catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value) + " with result infos " + infos.getInfos(), e);
		}

		return writer.writeRowToString(result);
	}
}
