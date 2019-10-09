package com.bakdata.conquery.models.datasets;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.WorkerStorage;
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
	@NotNull @Valid @JsonManagedReference
	private Column[] columns = new Column[0];
	
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

	public List<Import> findImports(WorkerStorage storage) {
		return storage
			.getAllImports()
			.stream()
			.filter(imp -> imp.getTable().equals(this.getId()))
			.collect(Collectors.toList());
	}
}
