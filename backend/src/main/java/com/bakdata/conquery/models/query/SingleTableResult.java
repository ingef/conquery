package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

public interface SingleTableResult {

	default List<ColumnDescriptor> generateColumnDescriptions(boolean isInitialized, Namespace namespace, ConqueryConfig config) {
		Preconditions.checkArgument(isInitialized, "The execution must have been initialized first");
		List<ColumnDescriptor> columnDescriptions = new ArrayList<>();

		final Locale locale = I18n.LOCALE.get();

		PrintSettings settings = new PrintSettings(true, locale, namespace, config, null);

		UniqueNamer uniqNamer = new UniqueNamer(settings);

		// First add the id columns to the descriptor list. The are the first columns
		for (ResultInfo header : config.getIdColumns().getIdResultInfos()) {
			columnDescriptions.add(ColumnDescriptor.builder()
												   .label(uniqNamer.getUniqueName(header))
												   .type(ResultType.StringT.getINSTANCE().typeInfo())
												   .semantics(header.getSemantics())
												   .build());
		}

		final UniqueNamer collector = new UniqueNamer(settings);
		getResultInfos().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(settings, collector)));
		return columnDescriptions;
	}

	@JsonIgnore
	List<ResultInfo> getResultInfos();

	Stream<EntityResult> streamResults();

	@JsonIgnore
	long resultRowCount();

}
