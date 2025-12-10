package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
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

	default List<ColumnDescriptor> generateColumnDescriptions(boolean isInitialized, ConqueryConfig config) {
		Preconditions.checkArgument(isInitialized, "The execution must have been initialized first");
		final List<ColumnDescriptor> columnDescriptions = new ArrayList<>();

		final Locale locale = I18n.LOCALE.get();
		// The printer is never used to generate results. But downstream code might touch them
		final PrintSettings settings = new PrintSettings(true, locale, getNamespace(), config, null, null);

		final UniqueNamer uniqNamer = new UniqueNamer(settings);

		// First add the id columns to the descriptor list. The are the first columns
		for (ResultInfo header : config.getIdColumns().getIdResultInfos()) {
			final ColumnDescriptor descriptor =
					new ColumnDescriptor(uniqNamer.getUniqueName(header, settings), null, null, ResultType.Primitive.STRING.typeInfo(), header.getSemantics());
			columnDescriptions.add(descriptor);
		}

		final UniqueNamer collector = new UniqueNamer(settings);
		collectResultInfos().forEach(info -> columnDescriptions.add(info.asColumnDescriptor(collector, settings)));
		return columnDescriptions;
	}

	@JsonIgnore
	Namespace getNamespace();

	/**
	 * Collect result infos from the submitted query that this execution wraps
	 * @return The result infos for this execution
	 * @implNote The execution may need to be initialised beforehand
	 */
	@JsonIgnore
	List<ResultInfo> collectResultInfos();

	/**
	 * Collect result infos from the submitted query that this execution wraps
	 * @return The result infos for this execution
	 */
	@JsonIgnore
	List<ResultInfo> getResultInfos();

	/**
	 * @param limit Optionally limits how many lines are emitted.
	 */
	Stream<EntityResult> streamResults(OptionalLong limit);

	@JsonIgnore
	OptionalLong resultRowCount();


}
