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

	private final Namespace namespace;

	public Stream<String> toCSV(ManagedQuery query) {
		if (query.getStatus() != QueryStatus.DONE) {
			throw new IllegalArgumentException("Can only create a CSV from a successfully finished Query" + query.getId());
		}
		return Stream.concat(Stream.of(Joiner.on(DELIMETER).join(ID_MAPPING.getPrintIdFields()) + DELIMETER + "dates"), createCSVBody(query));

	}

	private Stream<String> createCSVBody(ManagedQuery query) {
		return query.getResults()
			.stream()
			.filter(ContainedEntityResult.class::isInstance)
			.map(ContainedEntityResult.class::cast)
			.map(this::createCSVLine);
	}

	private String createCSVLine(ContainedEntityResult cer) {
		StringBuilder result = new StringBuilder();
		Dictionary dict = namespace.getStorage().getPrimaryDictionary();
		Joiner.on(DELIMETER).appendTo(result, ID_MAPPING.toExternal(new CsvEntityId(dict.getElement(cer.getEntityId())), namespace).getExternalId());
		result.append(DELIMETER);
		Joiner.on(DELIMETER).appendTo(result, cer.getValues());
		return result.toString();
	}
}
