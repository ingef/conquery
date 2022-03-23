package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
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
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private FilterTemplate template;


	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(filterType);


		f.setOptions(
				labels.entrySet().stream()
					  .map(entry -> new com.bakdata.conquery.apiv1.frontend.FEValue(entry.getKey(), entry.getValue()))
					  .collect(Collectors.toList())
		);
	}


	public static String decideColumnReference(Column column) {

		if (column.getSharedDictionary() != null) {
			return column.getSharedDictionary();
		}

		if (column.getSecondaryId() != null) {
			return column.getSecondaryId().getId().toString();
		}

		return column.getId().toString();
	}

	@JsonIgnore
	public List<String> getSearchReferences() {
		final List<String> references = new ArrayList<>(3);

		if (getTemplate() != null) {
			references.add(getTemplate().getFilePath());
		}

		references.add(getId().toString());
		references.add(decideColumnReference(getColumn()));

		return references;
	}
}
