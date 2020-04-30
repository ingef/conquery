package com.bakdata.conquery.models.forms.frontendconfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;

/**
 * Base class providers of frontend form configurations, which are automatically loaded by the {@link FormProcessor}.
 * The provided {@link ObjectReader} can be used to parse the configurations to {@link JsonNode}s.
 * Other constructors than the one provided here are not supported yet.
 */
@RequiredArgsConstructor
public abstract class FormFrontendConfigProviderBase implements FormFrontendConfigProvider{
	protected final ObjectReader reader;

}
