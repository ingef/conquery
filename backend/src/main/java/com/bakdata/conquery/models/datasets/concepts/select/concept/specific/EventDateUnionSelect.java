package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import java.util.Locale;
import java.util.stream.Collectors;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Collects the event dates that are corresponding to an enclosing {@link Connector} or {@link Concept} provided in a query.
 * The resulting date set is in bounds of a provided date restriction.
 */
@CPSType(id = "EVENT_DATE_UNION", base = Select.class)
public class EventDateUnionSelect extends UniversalSelect {

	/**
	 * Use cache to prevent byte[] allocation on every call to C10N.get(*I10n*.class, key).*element*()
	 */
	@JsonIgnore
	private final static LoadingCache<Locale,String> labelCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
		@Override
		public String load(Locale key) {
			return C10N.get(ResultHeadersC10n.class, key).dates();
		}
	});

	@Override
	public String getLabel() {
		return labelCache.getUnchecked(I18n.LOCALE.get());
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new EventDateUnionAggregator(getHolder().findConcept()
													   .getConnectors()
													   .stream()
													   .map(Connector::getTable)
													   .collect(Collectors.toSet()));
	}
}
