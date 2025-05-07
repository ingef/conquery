package com.bakdata.conquery.models.datasets;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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
public class Import extends NamespacedIdentifiable<ImportId> {

	private final String name;

	@Valid
	@NotNull
	private final TableId table;

	private long numberOfEntities;

	private long numberOfEntries;


	@JsonManagedReference
	@NotNull
	private ImportColumn[] columns = new ImportColumn[0];


	@Override
	public ImportId createId() {
		return new ImportId(table, getName());
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
	public DatasetId getDataset() {
		return getTable().getDataset();
	}

}
