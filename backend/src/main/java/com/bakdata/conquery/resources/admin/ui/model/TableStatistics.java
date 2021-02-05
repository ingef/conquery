package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class TableStatistics {
		
	private final Table table;
	private final long numberOfEntries;
	private final long dictionariesSize;
	private final long size;
	private final long cBlocksSize;
	private final List<Import> imports;
}
