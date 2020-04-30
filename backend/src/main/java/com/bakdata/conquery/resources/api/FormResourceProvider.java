package com.bakdata.conquery.resources.api;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormProcessor;
import com.bakdata.conquery.resources.ResourcesProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;

@CPSType(base = ResourcesProvider.class, id = "FORM_RESOURCES")
public class FormResourceProvider implements ResourcesProvider {

	private FormProcessor processor;
	
	@Override
	public void registerResources(MasterCommand master) {
		JerseyEnvironment environment = master.getEnvironment().jersey();
		processor = new FormProcessor(master.getStorage());

		environment.register(new FormResource(processor));
	}
}
