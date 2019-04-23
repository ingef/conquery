package com.bakdata.conquery.models.query;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.base.Joiner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryToCSVRenderer {

	private static final char DELIMETER = ConqueryConfig.getInstance().getCsv().getDelimeter();
	private static final IdMappingConfig ID_MAPPING = ConqueryConfig.getInstance().getIdMapping();
	private static final Joiner JOINER = Joiner.on(DELIMETER).useForNull("");
	private static final String HEADER = JOINER.join(ID_MAPPING.getPrintIdFields());

	private final Namespace namespace;

	public Stream<String> toCSV(ManagedQuery query) {
		return toCSV(new PrintSettings(), query);
	}
	
	public Stream<String> toCSV(PrintSettings cfg, ManagedQuery query) {
		if (query.getStatus() != QueryStatus.DONE) {
			throw new IllegalArgumentException("Can only create a CSV from a successfully finished Query" + query.getId());
		}
		List<ResultInfo> infos = query.getResultInfos();
		return Stream.concat(
			Stream.of(HEADER + DELIMETER + JOINER.join(infos.stream().map(ResultInfo::getUniqueName).iterator())),
			createCSVBody(cfg, infos, query)
		);
	}

	private Stream<String> createCSVBody(PrintSettings cfg, List<ResultInfo> infos, ManagedQuery query) {
		return query.getResults()
			.stream()
			.flatMap(ContainedEntityResult::filterCast)
			.map(result -> Pair.of(createId(result), result))
			.sorted(Comparator.comparing(Pair::getKey))
			.flatMap(res -> createCSVLine(cfg, infos, res));
	}

	private String createId(ContainedEntityResult cer) {
		Dictionary dict = namespace.getStorage().getPrimaryDictionary();
		return JOINER.join(
			ID_MAPPING
				.toExternal(new CsvEntityId(dict.getElement(cer.getEntityId())), namespace)
				.getExternalId()
		);
	}
	
	private Stream<String> createCSVLine(PrintSettings cfg, List<ResultInfo> infos, Pair<String, ContainedEntityResult> idResult) {
		return idResult
			.getValue()
			.streamValues()
			.map(result -> idResult.getKey() + DELIMETER + JOINER.join(print(cfg, infos, result)));
	}
	
	public static String[] print(PrintSettings cfg, List<ResultInfo> infos, Object[] value) {
		String[] result = new String[value.length];
		for(int i=0;i<infos.size();i++) {
			result[i] = infos.get(i).getType().printNullable(cfg, value[i]);
		}
		return result;
	}
}
