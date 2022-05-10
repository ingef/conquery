package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableCollection;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;

/**
 * Loads form frontend configuration from the bundled resources that are in the class path.
 * In order to be found, the configuration file name must end with {@code *.frontend_conf.json}.
 */
public class ResourceFormConfigProvider{

	public static void accept(ImmutableCollection.Builder<FormFrontendConfigInformation> formConfigInfos) {
		ResourceList frontendConfigs = CPSTypeIdResolver.SCAN_RESULT
			.getResourcesMatchingPattern(Pattern.compile(".*\\.frontend_conf\\.json"));
		
		for (Resource config : frontendConfigs) {
			try (config){
				try(InputStream in = config.open()){			
					JsonNode configTree = Jackson.getMapper().reader().readTree(in);
					formConfigInfos.add(new FormFrontendConfigInformation("Resource " + config.getPath(), configTree));
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException(String.format("Could not parse the frontend config: %s", config.getPath()), e);
			}
		}
		
	}
	
}