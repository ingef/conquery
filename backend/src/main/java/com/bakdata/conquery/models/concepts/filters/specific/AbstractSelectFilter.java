package com.bakdata.conquery.models.concepts.filters.specific;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.util.search.QuickSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> implements ISelectFilter {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();
	
	protected Set<String> values;
	@JsonIgnore
	protected transient QuickSearch<FilterSearchItem> sourceSearch;

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	public FilterSearch.FilterSearchType searchType = FilterSearch.FilterSearchType.EXACT;

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(filterType);
		// TODO: 20.11.2019 Upgrade to BigMultiSelect if more than maximumSize values are found.
		if (values != null) {
			if (maximumSize != -1 && values.size() > maximumSize) {
				throw new ConceptConfigurationException(getConnector(),
					String.format("Too many possible values (%d of %d in filter %s).", values.size(), maximumSize, this.getId()));
			}
			if(this.filterType != FEFilterType.BIG_MULTI_SELECT) {
				f.setOptions(
					values
						.stream()
						.map(v->new FEValue(getLabelFor(v), v))
						.collect(Collectors.toList())
				);
			}
		}
	}

	@Override
	public void addImport(Import imp) {
		if (values == null) {
			values = new HashSet<>();
		}
		values.addAll(Sets.newHashSet(((AStringType) getColumn().getTypeFor(imp)).iterator()));
	}

	public String getLabelFor(String value) {
		return labels.getOrDefault(value, value);
	}
	
	public String getValueFor(String label) {
		String value = labels.inverse().get(label);
		if(value == null) {
			if(values.contains(label)) {
				return label;
			}
		}
		return null;
	}
}
