package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingletonNamespaceCollection extends IdResolveContext {

	@NonNull
	private final CentralRegistry registry;

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) {
		return registry;
	}
}
