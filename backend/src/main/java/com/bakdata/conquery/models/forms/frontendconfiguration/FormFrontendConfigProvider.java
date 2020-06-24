package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.util.function.Consumer;

import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigProvider.FormFrontendConfigInformation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableCollection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Interface that frontend form configuration providers must implement in order to be loaded by the {@link FormProcessor}.
 * The provider is a Consumer that fills the consumed Collection with the configuration that it provides.
 * The configurations are wrapped in {@link FormFrontendConfigInformation}s to provide more meta informations in the logs.
 * Classes that implement this interface directly must have a NoArgsConstructor.
 * If the implementation should be provided with an {@link ObjectReader} see {@link FormFrontendConfigProviderBase}.
 */
@FunctionalInterface
public interface FormFrontendConfigProvider extends Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>>{
	
	@Data
	@AllArgsConstructor
	@ToString
	public static class FormFrontendConfigInformation {
		private String origin;
		/**
		 * The actual frontend configuration for the form.
		 */
		private JsonNode configTree;
	}
}
