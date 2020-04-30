package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.util.Collection;
import java.util.function.Consumer;

import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigProvider.FormFrontendConfigInformation;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Interface that frontend form configuration providers must implement in order to be loaded by the {@link FormProcessor}.
 * The provider is a Consumer that fills the consumed Collection with the configuration that it provides.
 * The configurations are wrapped in {@link FormFrontendConfigInformation}s to provide more meta informations in the logs
 *
 */
@FunctionalInterface
public interface FormFrontendConfigProvider extends Consumer<Collection<FormFrontendConfigInformation>>{
	
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
