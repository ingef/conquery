package com.bakdata.conquery.models.datasets;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportColumnId;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ImportColumn extends NamedImpl<ImportColumnId> {

	@JsonBackReference
	private Import parent;
	@NotNull @Valid
	private CType type;
	@Min(0)
	private int position = -1;
	
	@Override
	public ImportColumnId createId() {
		return new ImportColumnId(parent.getId(), getName());
	}
	
	@JsonIgnore
	public int getNullPosition() {
		if(type.getLines() == type.getNullLines()) {
			return -1;
		}
		int nullPosition = 0;
		for (ImportColumn col: parent.getColumns()) {
			if(col==this) {
				break;
			}
			if(col.type.requiresExternalNullStore()) {
				nullPosition++;
			}
		}
		return nullPosition;
	}
}
