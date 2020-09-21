package com.bakdata.conquery.models.preproc;


import java.util.StringJoiner;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for a preprocessed file when it is imported and its header needs to be initialized for a dataset.
 */
@Slf4j
public class InitializablePreprocessedHeader extends PreprocessedHeader {
	
	/**
	 * The table where the import corresponding to this header is imported to. This member is set upon 
	 * initializing the header with dataset.
	 */
	@JsonIgnore
	@Getter
	private transient Table targetTable = null;

	/**
	 * Initializes the columns of the header with the context of the provided dataset and asserts that the import fits to the dataset.
	 * This is done by matching the column of the import to the column the dataset schema.
	 */
	public void initDataset(Namespace namespace) {
		DatasetId datasetId = namespace.getDataset().getId();
		targetTable = namespace.getDataset().getTables().getOrFail(new TableId(namespace.getDataset().getId(), getTable()));
		getPrimaryColumn().getType().init(datasetId);
		for (PPColumn col : getColumns()) {
			col.getType().init(datasetId);
		}
		
		assertMatch(targetTable);
	}
	
	
	/**
	 * Generates an import id from the dataset context and header informations.
	 * @return
	 */
	public ImportId generateImportId() {
		Preconditions.checkNotNull(targetTable, "PreprocessedHeader was not initialized propperly. Call InitializablePreprocessedHeader::initDataset first.");
		return new ImportId(targetTable.getId(), getName());
	}

	/**
	 * Verify that the supplied table matches the preprocessed' data in shape.
	 */
	private void assertMatch(Table table) {
		StringJoiner errors = new StringJoiner("\n");

		if (!table.getPrimaryColumn().matches(getPrimaryColumn())) {
			errors.add(String.format("PrimaryColumn[%s] does not match table PrimaryColumn[%s]", getPrimaryColumn(), table.getPrimaryColumn()));
		}

		if (table.getColumns().length != getColumns().length) {
			errors.add(String.format("Length=`%d` does not match table Length=`%d`", getColumns().length, table.getColumns().length));
		}

		for (int i = 0; i < Math.min(table.getColumns().length, getColumns().length); i++) {
			if (!table.getColumns()[i].matches(getColumns()[i])) {
				errors.add(String.format("Column[%s] does not match table Column[%s]`", getColumns()[i], table.getColumns()[i]));
			}
		}

		if (errors.length() != 0) {
			log.error(errors.toString());
			throw new IllegalArgumentException(String.format("Headers[%s.%s.%s] do not match Table[%s]", getTable(), getName(), getSuffix(), table.getId()));
		}
	}
	
}
