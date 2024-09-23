package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

import com.bakdata.conquery.apiv1.FilterTemplate;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class FrontendValueIndexKey extends AbstractIndexKey<FrontendValueIndex> {


	private final int suffixCutoff;

	private final String splitPattern;


	/**
	 * @see FilterTemplate#getValue()
	 */
	private final String valueTemplate;


	/**
	 * @see FilterTemplate#getOptionValue()
	 */
	private final String optionValueTemplate;


	public FrontendValueIndexKey(URI csv, String internalColumn, String valueTemplate, String optionValueTemplate, int suffixCutoff, String splitPattern) {
		super(csv, internalColumn);
		this.suffixCutoff = suffixCutoff;
		this.splitPattern = splitPattern;

		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
	}

	@Override
	public List<String> getExternalTemplates() {
		return List.of(valueTemplate, optionValueTemplate);
	}

	@Override
	public FrontendValueIndex createIndex(String defaultEmptyLabel) {
		return new FrontendValueIndex(suffixCutoff, splitPattern, valueTemplate, optionValueTemplate, defaultEmptyLabel);
	}
}
