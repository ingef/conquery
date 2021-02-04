package com.bakdata.conquery.models.datasets;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportColumnId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class ImportColumn extends NamedImpl<ImportColumnId> {
	// TODO reduce usage of this class, it does nothing except hold a description
	@JsonBackReference @NotNull
	private final Import parent;

	@NotNull @Valid
	private final ColumnStore typeDescription;

	@Override
	public ImportColumnId createId() {
		return new ImportColumnId(parent.getId(), getName());
	}


}
