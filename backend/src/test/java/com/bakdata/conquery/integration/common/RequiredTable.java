package com.bakdata.conquery.integration.common;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.validators.ExistingFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequiredTable {

	@NotNull
	@NotEmpty
	private String name;
	@NotNull
	private ResourceFile csv;
	@NotNull
	@Valid
	private RequiredColumn primaryColumn;
	@NotEmpty
	@Valid
	private RequiredColumn[] columns;

	public Table toTable() {
		Table table = new Table();
		table.setName(name);
		table.setPrimaryColumn(primaryColumn.toColumn(table));
		table.setColumns(Arrays.stream(columns).map(col -> col.toColumn(table)).toArray(Column[]::new));
		return table;
	}
}
