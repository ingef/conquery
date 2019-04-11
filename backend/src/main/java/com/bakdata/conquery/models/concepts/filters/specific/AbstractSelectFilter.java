package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.IStringType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> implements ISelectFilter {

	protected Set<String> values;
	protected Map<String, String> labels = Collections.emptyMap();
	protected boolean matchLabels = false;
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

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(filterType);

//		if (maximumSize != -1 && values.size() > maximumSize) {
//			throw new ConceptConfigurationException(getConnector(),
//				String.format("Too many possible values (%d of %d in filter %s).", values.size(), maximumSize, this.getId()));
//		}
		if (values != null) {
			realLabels = values.stream().limit(200).collect(Collectors.toMap(Function.identity(), e -> {
				String r = labels.get(e);
				return r == null ? e : r;
			}));

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

	@Override
	public void addImport(Import imp) {
		if (values == null) {
			values = new HashSet<>();
		}
		values.addAll(Sets.newHashSet(((IStringType) getColumn().getTypeFor(imp)).iterator()));
	}
}
