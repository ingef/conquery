package com.bakdata.conquery.models.datasets;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class Column extends Labeled<ColumnId> implements NamespacedIdentifiable<ColumnId>, Searchable<ColumnId> {

	public static final int UNKNOWN_POSITION = -1;

	@JsonBackReference
	@NotNull
	@EqualsAndHashCode.Exclude
	private Table table;
	@NotNull
	private MajorTypeId type;

	@Nullable
	private String description;

	private int minSuffixLength = 3;
	private boolean generateSuffixes;
	private boolean searchDisabled = false;

	@JsonIgnore
	@Getter(lazy = true)
	private final int position = ArrayUtils.indexOf(getTable().getColumns(), this);
	/**
	 * if this is set this column counts as the secondary id of the given name for this
	 * table
	 */
	@NsIdRef
	private SecondaryIdDescription secondaryId;

	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}

	@Override
	public String toString() {
		return "Column(id = " + getId() + ", type = " + getType() + ")";
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return table.getDataset();
	}


	@Override
	public TrieSearch<FrontendValue> createTrieSearch(IndexConfig config, NamespaceStorage storage) {

		final int suffixLength = isGenerateSuffixes() ? config.getSearchSuffixLength() : Integer.MAX_VALUE;

		final TrieSearch<FrontendValue> search = new TrieSearch<>(suffixLength, config.getSearchSplitChars());

		// TODO send message to shards to collect data, then on response create trieSearch proper?


		return search;
	}
}
