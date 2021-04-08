package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingletonNamespaceCollection extends IdResolveContext {

	public SingletonNamespaceCollection(CentralRegistry registry) {
		this(registry, null);
	}

	@NonNull
	private final CentralRegistry registry;
	private final CentralRegistry metaRegistry;

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) {
		return registry;
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		return metaRegistry;
	}
}
