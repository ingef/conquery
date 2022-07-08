package com.bakdata.conquery.models.datasets.concepts.select.concept.specific;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDurationSumAggregator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@CPSType(id = "EVENT_DURATION_SUM", base = Select.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventDurationSumSelect extends UniversalSelect {

	/**
	 * Use cache to prevent byte[] allocation on every call to C10N.get(*I10n*.class, key).*element*()
	 */
	@JsonIgnore
	private final static LoadingCache<Locale,String> labelCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
		@Override
		public String load(Locale key) {
			return C10N.get(ResultHeadersC10n.class, I18n.LOCALE.get()).eventDuration();
		}
	});

	@Override
	public String getLabel() {
		return C10N.get(ResultHeadersC10n.class, I18n.LOCALE.get()).eventDuration();
	}


	@Override
	public Aggregator<?> createAggregator() {
		return new EventDurationSumAggregator();
	}

	public static EventDurationSumSelect create(String name) {
		Preconditions.checkArgument(StringUtils.isNotBlank(name), "The name of the select must not be blank");
		EventDurationSumSelect select = new EventDurationSumSelect();
		select.setName(name);
		return select;
	}
}
