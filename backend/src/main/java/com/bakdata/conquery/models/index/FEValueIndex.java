package com.bakdata.conquery.models.index;

import java.util.Map;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.util.search.TrieSearch;

public class FEValueIndex extends TrieSearch<FEValue> implements Index<FEValueIndexKey> {


	/**
	 * @see FilterTemplate#getValue()
	 */
	private final String valueTemplate;


	/**
	 * @see FilterTemplate#getOptionValue()
	 */
	private final String optionValueTemplate;

	public FEValueIndex(int suffixCutoff, String split, String valueTemplate, String optionValueTemplate) {
		super(suffixCutoff, split);
		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
	}

	@Override
	public void put(String internalValue, Map<String, String> templateToConcrete) {
		FEValue feValue = new FEValue(
				internalValue,
				templateToConcrete.get(valueTemplate),
				templateToConcrete.get(optionValueTemplate)
		);
		addItem(feValue, FilterSearch.extractKeywords(feValue));
	}

	@Override
	public long size() {
		return calculateSize();
	}


	@Override
	public void finalizer() {
		shrinkToFit();
	}
}
