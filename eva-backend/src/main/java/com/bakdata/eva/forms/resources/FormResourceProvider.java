package com.bakdata.eva.forms.resources;

import java.io.IOException;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.resources.ResourcesProvider;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(base = ResourcesProvider.class, id = "FORM_RESOURCES")
public class FormResourceProvider implements ResourcesProvider {

	private FormProcessor processor;
	
	@Override
	public void registerResources(MasterCommand master) {
		JerseyEnvironment environment = master.getEnvironment().jersey();
		processor = new FormProcessor(master.getNamespaces(), master.getStorage());

		environment.register(new FormResource(master.getNamespaces(), processor));
		environment.register(new StatisticResultResource(processor));
	}
	
	@Override
	public void close() throws IOException {
		processor.close();
	}
}
