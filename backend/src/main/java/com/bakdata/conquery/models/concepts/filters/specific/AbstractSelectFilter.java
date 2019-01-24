package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.messages.namespaces.specific.AddImport;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.IStringType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSelectFilter<FE_TYPE extends FilterValue<?>> extends SingleColumnFilter<FE_TYPE> implements ISelectFilter {

	protected Map<String, String> labels = Collections.emptyMap();
	protected boolean matchLabels = false;
	protected boolean allowDropFile = false;
	@JsonIgnore
	protected Map<String, String> realLabels;
	@JsonIgnore
	protected Map<String, String> labelsToRealLabels;
	@JsonIgnore
	protected transient QuickSearch sourceSearch;

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private Dictionary dictionary;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(filterType);

		if(maximumSize>0) {
			f.setOptions(FEValue.fromLabels(realLabels));
		}
	}

	public String resolveValueToRealValue(String value) {
		if (realLabels.containsKey(value)) {
			return value;
		}
		if (labelsToRealLabels.containsKey(value)) {
			return labelsToRealLabels.get(value);
		}
		return null;
	}
}
