package com.bakdata.conquery.models.index;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class FrontendValueIndex extends TrieSearch<FrontendValue> implements Index<FrontendValueIndexKey> {


	/**
	 * @see FilterTemplate#getValue()
	 */
	private final String valueTemplate;


	/**
	 * @see FilterTemplate#getOptionValue()
	 */
	private final String optionValueTemplate;
	private final String defaultEmptyLabel;

	public FrontendValueIndex(int suffixCutoff, String split, String valueTemplate, String optionValueTemplate, String defaultEmptyLabel1) {
		super(suffixCutoff, split);
		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
		this.defaultEmptyLabel = defaultEmptyLabel1;
	}

	@Override
	public void put(String internalValue, Map<String, String> templateToConcrete) {
		final FrontendValue feValue = new FrontendValue(
				internalValue,
				templateToConcrete.get(valueTemplate),
				templateToConcrete.get(optionValueTemplate)
		);
		addItem(feValue, FilterSearch.extractKeywords(feValue));
	}

	@Override
	public int size() {
		final long longSize = calculateSize();
		if (longSize > Integer.MAX_VALUE) {
			log.trace("Trie size was larger than an int. Reporting Integer.MAX_VALUE. Was actually: {}", longSize);
			return Integer.MAX_VALUE;
		}
		return (int) longSize;
	}


	@Override
	public void finalizer() {
		// If no empty label was provided by the mapping, we insert the configured default-label
		if (findExact(List.of(""), 1).isEmpty()) {
			addItem(new FrontendValue("", defaultEmptyLabel), List.of(defaultEmptyLabel));
		}

		shrinkToFit();
	}
}
