package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
@NoArgsConstructor
@Slf4j
@JsonIgnoreProperties({"searchType"})
public abstract class SelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> {

	@CheckForNull
	@Valid
	private Range.IntegerRange substringRange = null;

	/**
	 * user given mapping from the values in the columns to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();


	@View.ApiManagerPersistence
	private SearchIndexId template;
	private int searchMinSuffixLength = 3;
	private boolean generateSearchSuffixes = true;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final SearchIndexId searchIndexId = getTemplate();
		if (searchIndexId != null) {
			f.setTemplate((FilterTemplate) searchIndexId.resolve());
		}
		f.setType(getFilterType());

		// If either not searches are available or all are disabled, we allow users to supply their own values
		f.setCreatable(conqueryConfig.getFrontend().isAlwaysAllowCreateValue() || getSearchReferences().stream().noneMatch(Predicate.not(Searchable::isSearchDisabled)));

		f.setOptions(collectLabels());
	}

	@JsonIgnore
	public abstract String getFilterType();


	/**
	 * The actual Searchables to use, if there is potential for deduplication/pooling.
	 *
	 * @implSpec The order of objects returned is used to also sort search results from different sources.
	 */
	@JsonIgnore
	public List<Searchable> getSearchReferences() {
		final List<Searchable> out = new ArrayList<>();

		if (getTemplate() != null) {
			final FilterTemplate index = (FilterTemplate) getTemplate().resolve();
			if (!index.isSearchDisabled()) {
				out.add(index);
			}
		}

		if (!labels.isEmpty()) {
			out.add(new LabelMap(getId(), labels, searchMinSuffixLength, generateSearchSuffixes));
		}

		final Column column = getColumn().resolve();
		if (!column.isSearchDisabled()) {
			out.add(column);
		}

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

	@JsonIgnore
	@ValidationMethod(message = "Substrings must start at 0.")
	public boolean isMinPositive() {
		if (getSubstringRange() == null) {
			return true;
		}
		if (getSubstringRange().getMin() == null) {
			return true;
		}

		return getSubstringRange().getMin() >= 0;
	}
}
