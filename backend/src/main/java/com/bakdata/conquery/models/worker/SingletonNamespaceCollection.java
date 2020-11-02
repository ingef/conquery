package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingletonNamespaceCollection extends IdResolveContext {
	private final CentralRegistry registry;

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) {
		return registry;
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		return registry;
	}
}
