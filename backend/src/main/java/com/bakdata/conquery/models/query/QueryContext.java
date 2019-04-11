package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

@Getter @AllArgsConstructor @RequiredArgsConstructor
@Wither
public class QueryContext {

	private Column validityDateColumn;
	@NonNull
	private CDateSet dateRestriction = CDateSet.createFull();
	private boolean prettyPrint = true;
	private final WorkerStorage storage;
}
