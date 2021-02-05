package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class ImportStatistics {
		
	private final Import imp;
	private final long cBlocksMemoryBytes;
}
