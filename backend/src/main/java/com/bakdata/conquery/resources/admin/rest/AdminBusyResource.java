package com.bakdata.conquery.resources.admin.rest;

import jakarta.inject.Inject;

import com.bakdata.conquery.models.api.openapi.BusyApi;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminBusyResource implements BusyApi {

	private final AdminProcessor processor;

	@Override
	public Boolean busyGet() {
		return processor.isBusy();
	}
}
