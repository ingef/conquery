package com.bakdata.conquery.integration.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

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
	@JsonIgnore
	private String importName;

	@JsonCreator
	public static RequiredTable fromFile(String fileResource) throws IOException {
		return Jackson.MAPPER.readValue(
				Objects.requireNonNull(
						IntegrationTest.class.getResourceAsStream(fileResource),
						fileResource + " not found"
				),
				RequiredTable.class
		);
	}

	public Table toTable(DatasetId dataset, NamespacedStorageProvider idResolver) {
		Table table = new Table();
		table.setPrimaryColumn(primaryColumn.toColumn(table, idResolver));
		table.setDataset(dataset);
		table.setName(name);
		table.setColumns(Arrays.stream(columns)
							   .map(col -> col.toColumn(table, idResolver)).toArray(Column[]::new));

		return table;
	}
}
