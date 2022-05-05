package com.bakdata.conquery.models.datasets;

import java.util.Arrays;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class SecondaryIdDescription extends Labeled<SecondaryIdDescriptionId> implements NamespacedIdentifiable<SecondaryIdDescriptionId>, Searchable {

	@NsIdRef
	private Dataset dataset;

	private String description;

	private int searchMinSuffixLength = 3;
	private boolean generateSearchSuffixes = true;

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

	@Override
	public Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage) {
		return storage.getTables().stream()
					  .map(Table::getColumns)
					  .flatMap(Arrays::stream)
					  .filter(column -> this.equals(column.getSecondaryId()))
					  .flatMap(column -> column.getSearchValues(config, storage));
	}

	@Override
	public SecondaryIdDescriptionId createId() {
		return new SecondaryIdDescriptionId(dataset.getId(), getName());
	}

	@Override
	public String toString() {
		return "SecondaryIdDescription(id = " + getId() + ", label = " + getLabel() + " )";
	}
}
