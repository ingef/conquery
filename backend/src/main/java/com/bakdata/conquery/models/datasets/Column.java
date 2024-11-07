package com.bakdata.conquery.models.datasets;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
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
public class Column extends Labeled<ColumnId> implements NamespacedIdentifiable<ColumnId>, Searchable {

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
	private int position = -1;

	/**
	 * if this is set this column counts as the secondary id of the given name for this
	 * table
	 */
	private SecondaryIdDescriptionId secondaryId;

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return table.getDataset();
	}

	@Override
	public String toString() {
		return "Column(id = " + getId() + ", type = " + getType() + ")";
	}

	/**
	 * We create only an empty search here, because the content is provided through {@link com.bakdata.conquery.models.messages.namespaces.specific.RegisterColumnValues} and filled by the caller.
	 */
	@Override
	public TrieSearch<FrontendValue> createTrieSearch(IndexConfig config) {
		return config.createTrieSearch(isGenerateSuffixes());
	}

	public void init() {
		if (getPosition() >= 0) {
			// Column was initialized
			return;
		}

		position = ArrayUtils.indexOf(getTable().getColumns(), this);
	}

	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}
}
