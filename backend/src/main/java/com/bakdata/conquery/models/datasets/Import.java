package com.bakdata.conquery.models.datasets;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.preproc.PPColumn;
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
	private final TableId table;

	private long numberOfEntries;

	@JsonManagedReference
	@NotNull
	private ImportColumn[] columns = new ImportColumn[0];

	@NotNull
	private Set<DictionaryId> dictionaries;

	public static Import createForPreprocessing(String table, String tag, PPColumn[] columns) {
		Import imp = new Import(
				new TableId(new DatasetId("preprocessing"), table)
		); // is not yet used here.
		imp.setName(tag);
		ImportColumn[] impCols = new ImportColumn[columns.length];
		for (int c = 0; c < impCols.length; c++) {
			ImportColumn col = new ImportColumn();
			col.setName(columns[c].getName());
			col.setParent(imp);
			col.setPosition(c);
			col.setType(columns[c].getType());
			impCols[c] = col;
		}
		imp.setColumns(impCols);

		return imp;
	}

	@Override
	public ImportId createId() {
		return new ImportId(table, getName());
	}

	public void loadExternalInfos(NamespacedStorage storage) {
		for (ImportColumn col : columns) {
			col.getType().loadDictionaries(storage);
		}
	}

	public long estimateMemoryConsumption() {
		long mem = 0;
		for (ImportColumn col : columns) {
			mem += col.getType().estimateMemoryConsumption();
		}
		return mem;
	}

}
