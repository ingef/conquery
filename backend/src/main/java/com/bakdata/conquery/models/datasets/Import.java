package com.bakdata.conquery.models.datasets;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonCreator;
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
public class Import extends NamedImpl<ImportId> {


	@Valid
	@NotNull
	private final TableId table; // todo migrate to NsIdRef

	private long numberOfEntities;

	private long numberOfEntries;


	@JsonManagedReference
	@NotNull
	private ImportColumn[] columns = new ImportColumn[0];

	@NotNull
	private Set<DictionaryId> dictionaries;

	@Override
	public ImportId createId() {
		return new ImportId(table, getName());
	}

	public void loadExternalInfos(NamespacedStorage storage) {
		for (ImportColumn col : columns) {

			if(col.getTypeDescription() instanceof StringStore) {
				((StringStore) col.getTypeDescription()).loadDictionaries(storage);
			}
		}
	}

	public long estimateMemoryConsumption() {
		long mem = 0;
		for (ImportColumn col : columns) {
			mem += col.getMemorySizeBytes();
		}
		return mem;
	}

}
