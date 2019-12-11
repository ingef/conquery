package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@RequiredArgsConstructor
public class QueryToCSVRenderer {

	private static final IdMappingConfig ID_MAPPING = ConqueryConfig.getInstance().getIdMapping();
	private static final Collection<String> HEADER = Arrays.asList(ID_MAPPING.getPrintIdFields());
	private static final PrintSettings PRINT_SETTINGS = new PrintSettings(true, ConqueryConfig.getInstance().getCsv().getColumnNamerScript());
	
	public Stream<String> toCSV(ManagedQuery query, Map<String, Object> mappingState) {
		return toCSV(PRINT_SETTINGS, query, mappingState);
	}
	
	public Stream<String> toCSV(PrintSettings cfg, ManagedQuery query, Map<String, Object> mappingState) {
		return toCSV(cfg, List.of(query), mappingState);
	}
	
	public Stream<String> toCSV(PrintSettings cfg, List<ManagedQuery> queries, Map<String, Object> mappingState) {
		if (queries.stream()
			.anyMatch(q -> q.getState() != ExecutionState.DONE)) {
			throw new IllegalArgumentException("Can only create a CSV from a successfully finished Query " + queries.iterator().next().getId());
		}

		ResultInfoCollector infos = queries.iterator().next().collectResultInfos(cfg);
		
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
						q.collectResultInfos(cfg),
						q,
						mappingState))
		);
	}

	private Stream<String> createCSVBody(CsvWriter writer, PrintSettings cfg, ResultInfoCollector infos, ManagedQuery query, Map<String, Object> mappingState) {
		Namespace namespace = Objects.requireNonNull(query.getNamespace());
		return query.getResults()
			.stream()
			.flatMap(ContainedEntityResult::filterCast)
			.map(
				result -> Pair
					.of(createId(namespace, result, mappingState), result))
			.sorted(Comparator.comparing(Pair::getKey))
			.flatMap(res -> createCSVLine(writer, cfg, infos, res));
	}

	private ExternalEntityId createId(Namespace namespace, ContainedEntityResult cer, Map<String, Object> mappingState) {
		DirectDictionary dict = namespace.getStorage().getPrimaryDictionary();
		return ID_MAPPING
			.toExternal(
				new CsvEntityId(dict.getElement(cer.getEntityId())),
				namespace,
				mappingState);
	}
	
	private Stream<String> createCSVLine(CsvWriter writer, PrintSettings cfg, ResultInfoCollector infos, Pair<ExternalEntityId, ContainedEntityResult> idResult) {
		return idResult
			.getValue()
			.streamValues()
			.map(result -> print(writer, cfg, idResult.getKey(), infos, result));
	}
	
	public static String print(CsvWriter writer, PrintSettings cfg, ExternalEntityId entity, ResultInfoCollector infos, Object[] value) {
		List<String> result = new ArrayList<>(entity.getExternalId().length + value.length);
		result.addAll(Arrays.asList(entity.getExternalId()));
		for(int i=0;i<infos.size();i++) {
			result.add(infos.getInfos().get(i).getType().printNullable(cfg, value[i]));
		}
		return writer.writeRowToString(result);
	}
}
