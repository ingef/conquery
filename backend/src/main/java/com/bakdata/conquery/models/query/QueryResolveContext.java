package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.worker.Namespace;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class QueryResolveContext {
	private final MasterMetaStorage storage;
	private final Namespace namespace;
}
