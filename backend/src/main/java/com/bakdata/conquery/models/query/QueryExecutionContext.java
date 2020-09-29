package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter @AllArgsConstructor @RequiredArgsConstructor
@With
public class QueryExecutionContext {

	private Column validityDateColumn;
	@NonNull
	private CDateSet dateRestriction = CDateSet.createFull();
	private boolean prettyPrint = true;
	private Connector connector;
	private final WorkerStorage storage;

}