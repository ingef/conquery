package com.bakdata.conquery.models.datasets;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportColumnId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class ImportColumn extends NamespacedIdentifiable<ImportColumnId> {
	// TODO reduce usage of this class, it does nothing except hold a description
	@JsonBackReference
	@NotNull
	@EqualsAndHashCode.Exclude
	private final Import parent;

	// Only used on ManagerNode for com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter.addImport
	// Can Probably be removed.
	private final ColumnStore typeDescription;

	private final long lines;

	@Min(0)
	private final long memorySizeBytes;

	@Getter(onMethod_ = {@ToString.Include, @NotBlank})
	@Setter
	private String name;


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
	public DatasetId getDataset() {
		return parent.getDataset();
	}

}
