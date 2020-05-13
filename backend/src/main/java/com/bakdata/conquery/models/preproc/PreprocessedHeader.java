package com.bakdata.conquery.models.preproc;

import java.util.StringJoiner;

import com.bakdata.conquery.models.datasets.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data @NoArgsConstructor @AllArgsConstructor @Slf4j
public class PreprocessedHeader {
	private String name;
	private String table;
	private String suffix;

	private long rows;
	private long groups;
	private PPColumn primaryColumn;
	private PPColumn[] columns;

	private int validityHash;


	/**
	 * Verify that the supplied table matches the preprocessed' data in shape.
	 */
	public void assertMatch(Table table) {
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
