package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
@NoArgsConstructor
@Slf4j
@JsonIgnoreProperties({"searchType"})
public abstract class SelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> implements Searchable<FilterId> {

	/**
	 * user given mapping from the values in the columns to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();


	@NsIdRef
	@View.ApiManagerPersistence
	private FilterTemplate template;
	private int searchMinSuffixLength = 3;
	private boolean generateSearchSuffixes = true;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(getFilterType());

		// If either not searches are available or all are disabled, we allow users to supply their own values
		f.setCreatable(conqueryConfig.getFrontend().isAlwaysAllowCreateValue() || getSearchReferences().stream().noneMatch(Predicate.not(Searchable::isSearchDisabled)));

		f.setOptions(collectLabels());
	}

	@JsonIgnore
	public abstract String getFilterType();

	@Override
	public List<Searchable<?>> getSearchReferences() {
		final List<Searchable<?>> out = new ArrayList<>();

		if (getTemplate() != null) {
			out.add(getTemplate());
		}

		if (!labels.isEmpty()) {
			out.add(this);
		}

		out.addAll(getColumn().getSearchReferences());

		return out;
	}

	@NotNull
	protected List<FrontendValue> collectLabels() {
		return labels.entrySet().stream()
					 .map(entry -> new FrontendValue(entry.getKey(), entry.getValue()))
					 .collect(Collectors.toList());
	}

	@JsonIgnore
	@ValidationMethod(message = "Cannot use both labels and template.")
	public boolean isNotUsingTemplateAndLabels() {
		// Technically it's possible it just doesn't make much sense and would lead to Single-Point-of-Truth confusion.
		if (getTemplate() == null && labels.isEmpty()) {
			return true;
		}

		return (getTemplate() == null) != labels.isEmpty();
	}

	@Override
	@JsonIgnore
	public boolean isGenerateSuffixes() {
		return generateSearchSuffixes;
	}

	@Override
	@JsonIgnore
	public int getMinSuffixLength() {
		return searchMinSuffixLength;
	}

	/**
	 * Does not make sense to distinguish at Filter level since it's only referenced when labels are set.
	 */
	@Override
	@JsonIgnore
	public boolean isSearchDisabled() {
		return false;
	}

	@Override
	public TrieSearch<FrontendValue> createTrieSearch(IndexConfig config, NamespaceStorage storage) {

		final TrieSearch<FrontendValue> search = config.createTrieSearch(true);

		if(log.isTraceEnabled()) {
			log.trace("Labels for {}: `{}`", getId(), collectLabels().stream().map(FrontendValue::toString).collect(Collectors.toList()));
		}

		StopWatch timer = StopWatch.createStarted();
		log.trace("START-SELECT ADDING_ITEMS for {}", getId());

		collectLabels().forEach(feValue -> search.addItem(feValue, FilterSearch.extractKeywords(feValue)));

		log.trace("DONE-SELECT ADDING_ITEMS for {} in {}", getId(), timer);

		timer.reset();
		log.trace("START-SELECT SHRINKING for {}", getId());

		search.shrinkToFit();

		log.trace("DONE-SELECT SHRINKING for {} in {}", getId(), timer);

		return search;
	}
}
