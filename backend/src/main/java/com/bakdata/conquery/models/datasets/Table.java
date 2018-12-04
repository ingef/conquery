package com.bakdata.conquery.models.datasets;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.base.Preconditions;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Table extends Labeled<TableId> {
	
	@JsonBackReference
	private Dataset dataset;
	@NotNull @Valid @JsonManagedReference
	private Column primaryColumn;
	@NotEmpty @Valid @JsonManagedReference
	private Column[] columns = new Column[0];
	private Set<String> tags = new HashSet<>();
	
	public boolean matches(PPHeader header) {
		if(!primaryColumn.matches(header.getPrimaryColumn())) {
			return false;
		}
		if(columns.length != header.getColumns().length) {
			return false;
		}
		for(int i=0;i<columns.length;i++) {
			if(!columns[i].matches(header.getColumns()[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TableId createId() {
		return new TableId(Preconditions.checkNotNull(dataset.getId()), getName());
	}
}
