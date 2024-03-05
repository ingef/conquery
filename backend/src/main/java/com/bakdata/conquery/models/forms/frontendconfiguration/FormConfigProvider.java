package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableCollection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Helper class to hold the identifier and the provider method for a form config provider
 */
@RequiredArgsConstructor
public class FormConfigProvider {
	@Getter
	private final String providerName;
	private final Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> provider;

	public void addFormConfigs(ImmutableCollection.Builder<FormFrontendConfigInformation> collect) {
		provider.accept(collect);
	}
}
