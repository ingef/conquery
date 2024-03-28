package com.bakdata.conquery.models.datasets;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Description of imported data.
 */
@Getter
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@Setter
public class Import extends NamedImpl<ImportId> implements NamespacedIdentifiable<ImportId> {

	@Valid
	@NotNull
	@NsIdRef
	private final Table table;

	private long numberOfEntities;

	private long numberOfEntries;


	@JsonManagedReference
	@NotNull
	private ImportColumn[] columns = new ImportColumn[0];

	@Override
	public ImportId createId() {
		return new ImportId(table.getId(), getName());
	}

	public long estimateMemoryConsumption() {
		long mem = 0;
		for (ImportColumn col : columns) {
			mem += col.getMemorySizeBytes();
		}
		return mem;
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getTable().getDataset();
	}
}
