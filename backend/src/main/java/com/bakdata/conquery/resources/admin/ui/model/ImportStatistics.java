package com.bakdata.conquery.resources.admin.ui.model;


import com.bakdata.conquery.models.datasets.Import;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ImportStatistics {

	private final Import imp;
	private final long cBlocksMemoryBytes;
}
