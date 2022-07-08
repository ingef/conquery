package com.bakdata.conquery.resources.api;

import javax.inject.Inject;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormProcessor;
import com.bakdata.conquery.resources.ResourcesProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.RequiredArgsConstructor;

@CPSType(base = ResourcesProvider.class, id = "FORM_RESOURCES")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FormResourceProvider implements ResourcesProvider {

	private final FormProcessor processor;
	
	@Override
	public void registerResources(ManagerNode manager) {
		JerseyEnvironment environment = manager.getEnvironment().jersey();
	}
}
