package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.search.QuickSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();
	
	protected Set<String> values = new HashSet<>();
	@JsonIgnore
	protected transient QuickSearch<FilterSearchItem> sourceSearch;

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private FilterTemplate template;


	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	public FilterSearch.FilterSearchType searchType = FilterSearch.FilterSearchType.EXACT;

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(filterType);

		if (values == null || values.isEmpty()) {
			return;
		}

		if (maximumSize != -1 && values.size() > maximumSize) {
			log.warn("Too many possible values ({} of {} in Filter[{}]). Upgrading to BigMultiSelect", values.size(), maximumSize, getId());
			f.setType(FEFilterType.BIG_MULTI_SELECT);
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

	@Override
	public void addImport(Import imp) {
		if (values == null) {
			values = new HashSet<>();
		}

		final ColumnStore store = getColumn().getTypeFor(imp);

		values.addAll(Sets.newHashSet(((StringStore) store).iterator()));
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
