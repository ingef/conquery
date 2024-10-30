package com.bakdata.conquery.models.index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

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
	private final TrieSearch<FrontendValue> delegate;

	public FrontendValueIndex(int suffixCutoff, String split, String valueTemplate, String optionValueTemplate, String defaultEmptyLabel) {
		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
		this.defaultEmptyLabel = defaultEmptyLabel;

		delegate = new TrieSearch<>(suffixCutoff, split);
	}

	@Override
	public void put(String internalValue, Map<String, String> templateToConcrete) {
		final FrontendValue feValue = new FrontendValue(
				internalValue,
				templateToConcrete.get(valueTemplate),
				templateToConcrete.get(optionValueTemplate)
		);

		delegate.addItem(feValue, FilterSearch.extractKeywords(feValue));
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
	public Collection<FrontendValue> externalMultiple(String key, FrontendValue defaultValue) {
		return delegate.findExact(Set.of(key), Integer.MAX_VALUE);
	}

	@Override
	public FrontendValue external(String key, FrontendValue defaultValue) {
		return delegate.findExact(Set.of(key), 1).iterator().next();
	}

	@Override
	public void finalizer() {

		StopWatch timer = StopWatch.createStarted();

		// If no empty label was provided by the mapping, we insert the configured default-label
		if (delegate.findExact(List.of(""), 1).isEmpty()) {
			delegate.addItem(new FrontendValue("", defaultEmptyLabel), List.of(defaultEmptyLabel));
		}

		log.trace("DONE-FINALIZER ADDING_ITEMS in {}", timer);

		timer.reset();
		log.trace("START-FV-FIN SHRINKING");

		delegate.shrinkToFit();

		log.trace("DONE-FV-FIN SHRINKING in {}", timer);

	}
}
