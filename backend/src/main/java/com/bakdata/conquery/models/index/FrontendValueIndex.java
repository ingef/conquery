package com.bakdata.conquery.models.index;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@ToString
public class FrontendValueIndex implements Index<FrontendValue> {


	/**
	 * @see FilterTemplate#getValue()
	 */
	private final String valueTemplate;


	/**
	 * @see FilterTemplate#getOptionValue()
	 */
	private final String optionValueTemplate;
	private final String defaultEmptyLabel;

	@Getter
	private final Search<FrontendValue> delegate;

	public FrontendValueIndex(Search<FrontendValue> delegate, String valueTemplate, String optionValueTemplate, String defaultEmptyLabel) {
		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
		this.defaultEmptyLabel = defaultEmptyLabel;

		this.delegate = delegate;
	}

	@Override
	public void put(String internalValue, Map<String, String> templateToConcrete) {
		final FrontendValue feValue = new FrontendValue(
				internalValue,
				templateToConcrete.get(valueTemplate),
				templateToConcrete.get(optionValueTemplate)
		);

		delegate.addItem(feValue, SearchProcessor.extractKeywords(feValue));
	}

	@Override
	public int size() {
		final long longSize = delegate.calculateSize();
		if (longSize > Integer.MAX_VALUE) {
			log.trace("Trie size was larger than an int. Reporting Integer.MAX_VALUE. Was actually: {}", longSize);
			return Integer.MAX_VALUE;
		}
		return (int) longSize;
	}

	@Override
	public Collection<FrontendValue> externalMultiple(String key) {
		final List<FrontendValue> matches = delegate.findExact(key, Integer.MAX_VALUE);
		if (matches.isEmpty()) {
			return null;
		}
		return matches;
	}

	@Override
	public FrontendValue external(String key) {
		final List<FrontendValue> matches = delegate.findExact(key, 1);

		if (matches.isEmpty()) {
			return null;
		}

		return matches.getFirst();
	}

	@Override
	public void finalizer() {

		final StopWatch timer = StopWatch.createStarted();

		log.trace("DONE-FINALIZER ADDING_ITEMS in {}", timer);

		timer.reset();
		log.trace("START-FV-FIN SHRINKING");

		delegate.finalizeSearch();

		log.trace("DONE-FV-FIN SHRINKING in {}", timer);

	}
}
