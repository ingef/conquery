package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingletonNamespaceCollection extends IdResolveContext {

	@NonNull
	private final NsIdResolver resolver;

	@Override
	public NsIdResolver findIdResolver(DatasetId dataset) {
		return resolver;
	}
}
