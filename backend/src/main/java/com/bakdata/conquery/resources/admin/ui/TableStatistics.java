package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.models.datasets.Table;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class TableStatistics {
		
	private final Table table;
	private final long numberOfBlocks;
	private final long numberOfEntries;
}
