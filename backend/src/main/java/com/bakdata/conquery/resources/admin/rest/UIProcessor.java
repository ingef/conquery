package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UIProcessor {

	@Getter
	private final DatasetRegistry datasetRegistry;


	public UIContext getUIContext() {
		return new UIContext(datasetRegistry);
	}

}
