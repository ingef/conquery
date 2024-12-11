package com.bakdata.conquery.models.index;

import java.net.URI;
import java.util.List;

import com.bakdata.conquery.apiv1.FilterTemplate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class FrontendValueIndexKey implements IndexKey {


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
	@Getter
	private final URI csv;
	@Getter
	private final String internalColumn;


	public FrontendValueIndexKey(URI csv, String internalColumn, String valueTemplate, String optionValueTemplate, int suffixCutoff, String splitPattern) {
		this.suffixCutoff = suffixCutoff;
		this.splitPattern = splitPattern;

		this.valueTemplate = valueTemplate;
		this.optionValueTemplate = optionValueTemplate;
		this.csv = csv;
		this.internalColumn = internalColumn;
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
