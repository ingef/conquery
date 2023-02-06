package com.bakdata.conquery.models.datasets;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportColumnId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class ImportColumn extends NamedImpl<ImportColumnId> implements NamespacedIdentifiable<ImportColumnId> {
	// TODO reduce usage of this class, it does nothing except hold a description
	@JsonBackReference @NotNull
	private final Import parent;

	// Only used on ManagerNode for com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter.addImport
	// Can Probably be removed.
	private final ColumnStore typeDescription;

	private final long lines;

	@Min(0)
	private final long memorySizeBytes;


	@Override
	public ImportColumnId createId() {
		return new ImportColumnId(parent.getId(), getName());
	}

	@Override
	public String toString() {
		return "ImportColumn(id=" + getId() + ", typeDescription=" + getTypeDescription() + ")";
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return parent.getDataset();
	}
}
