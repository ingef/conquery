package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;

/**
 * Loads form frontend configuration from the bundled resources that are in the class path.
 * In order to be found, the configuration file name must end with {@code *.frontend_conf.json}.
 */
public class ResourceFormConfigProvider extends FormFrontendConfigProviderBase{
	

	public ResourceFormConfigProvider(ObjectReader reader) {
		super(reader);
	}

	@Override
	public void accept(Collection<FormFrontendConfigInformation> formConfigInfos) {
		ResourceList frontendConfigs = CPSTypeIdResolver.SCAN_RESULT
			.getResourcesMatchingPattern(Pattern.compile(".*\\.frontend_conf\\.json"));
		
		for (Resource config : frontendConfigs) {
			try {
				JsonNode configTree = reader.readTree(config.open());
				formConfigInfos.add(new FormFrontendConfigInformation("Resource " + config.getPath(), configTree));
			}
			catch (IOException e) {
				throw new IllegalArgumentException(String.format("Could not parse the frontend config: %s", config.getPath()), e);
			}
		}
		
	}
	
}