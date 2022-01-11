package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.apiv1.FilterSearchItem;
import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.search.QuickSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true, value = {"values"}) //TODO this is a hotfix because we cannot reimport this late
public abstract class AbstractSelectFilter extends SingleColumnFilter<String[]> {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();

	@JsonIgnore
	protected Set<String> values = new HashSet<>();

	@JsonIgnore
	protected transient QuickSearch<FilterSearchItem> sourceSearch;

	@JsonIgnore
	private final int maximumSize;

	@JsonIgnore
	private final Class<? extends FilterValue<String[]>> filterType;

	private FilterTemplate template;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	public FilterSearch.FilterSearchType searchType = FilterSearch.FilterSearchType.EXACT;

	public Class<? extends FilterValue<String[]>> getFilterType() {
		// Big MultiSelects are rendered differently in frontend
		if (isBig()) {
			return FilterValue.CQBigMultiSelectFilter.class;
		}

		return filterType;
	}

	@JsonIgnore
	private boolean isBig() {
		return getMaximumSize() != -1 && getValues().size() > getMaximumSize();
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());

		if (values == null || values.isEmpty()) {
			return;
		}

		if (isBig()) {
			f.setOptions(
					getValues().stream()
							.map(v -> new FEValue(getLabelFor(v), v))
							.collect(Collectors.toList())
			);
		}
	}

	@Override
	public void addImport(Import imp) {
		if (values == null) {
			values = new HashSet<>();
		}

		//TODO this is horrendously slow, better to directly fill the Search instead of copying twice.
		final ColumnStore store = getColumn().getTypeFor(imp);
		((StringStore) store).iterator().forEachRemaining(values::add);
	}

	public String getLabelFor(String value) {
		return labels.getOrDefault(value, value);
	}

	public String getValueFor(String label) {
		String value = labels.inverse().get(label);
		if (value == null) {
			if (values.contains(label)) {
				return label;
			}
		}
		return null;
	}
}
