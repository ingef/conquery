package com.bakdata.conquery.io.external.form;

import com.codahale.metrics.health.HealthCheck;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExternalFormBackendHealthCheck extends HealthCheck {

	private final ExternalFormBackendApi externalApi;

	@Override
	protected Result check() throws Exception {
		return externalApi.checkHealth();
	}
}
