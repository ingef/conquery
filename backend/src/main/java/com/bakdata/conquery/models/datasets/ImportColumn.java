package com.bakdata.conquery.models.datasets;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportColumnId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ImportColumn extends NamedImpl<ImportColumnId> {
	// TODO reduce usage of this class, it does nothing except hold a description
	@JsonBackReference @NotNull
	private Import parent;
	@NotNull @Valid
	private ColumnStore<?> type;
	@Min(0)
	private int position = -1;
	
	@Override
	public ImportColumnId createId() {
		return new ImportColumnId(parent.getId(), getName());
	}


}
