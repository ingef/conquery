package com.bakdata.conquery.models.query;

import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.base.Joiner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryToCSVRenderer {

	private static final char DELIMETER = ConqueryConfig.getInstance().getCsv().getDelimeter();
	private static final IdMappingConfig ID_MAPPING = ConqueryConfig.getInstance().getIdMapping();
	private static final Joiner JOINER = Joiner.on(DELIMETER);
	private static final String HEADER = JOINER.join(ID_MAPPING.getPrintIdFields());

	private final Namespace namespace;

	public Stream<String> toCSV(ManagedQuery query) {
		if (query.getStatus() != QueryStatus.DONE) {
			throw new IllegalArgumentException("Can only create a CSV from a successfully finished Query" + query.getId());
		}
		return Stream.concat(
			Stream.of(HEADER + DELIMETER + JOINER.join(query.getResultHeader())), 
			createCSVBody(query)
		);
	}

	private Stream<String> createCSVBody(ManagedQuery query) {
		return query.getResults()
			.stream()
			.flatMap(ContainedEntityResult::filterCast)
			.flatMap(this::createCSVLine);
	}

	private Stream<String> createCSVLine(ContainedEntityResult cer) {
		Dictionary dict = namespace.getStorage().getPrimaryDictionary();
		String idPart = JOINER.join(
			ID_MAPPING
				.toExternal(new CsvEntityId(dict.getElement(cer.getEntityId())), namespace)
				.getExternalId()
		);
		return cer
			.streamValues()
			.map(result -> idPart + DELIMETER + JOINER.join(result));
	}
}
