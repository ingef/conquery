package com.bakdata.conquery.models.datasets;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Table extends Labeled<TableId> {

	// TODO: 10.01.2020 fk: register imports here?

	@JsonBackReference
	private Dataset dataset;
	@NotNull @Valid @JsonManagedReference
	private Column[] columns = new Column[0];

	@Override
	public TableId createId() {
		return new TableId(Preconditions.checkNotNull(dataset.getId()), getName());
	}

	public List<Import> findImports(NamespacedStorage storage) {
		return storage
			.getAllImports()
			.stream()
			.filter(imp -> imp.getTable().equals(this.getId()))
			.collect(Collectors.toList());
	}
}
