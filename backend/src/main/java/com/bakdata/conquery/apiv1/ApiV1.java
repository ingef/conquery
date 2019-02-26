package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourcesProvider;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(base = ResourcesProvider.class, id = "ApiV1")
public class ApiV1 implements ResourcesProvider {

	@Override
	public void registerResources(MasterCommand master) {
		Namespaces namespaces = master.getNamespaces();
		JerseyEnvironment environment = master.getEnvironment().jersey();

		environment.register(master.getAuthDynamicFeature());
		environment.register(new ContentTreeResources(namespaces));
		environment.register(new ImportResource(namespaces));
		environment.register(new QueryResource(namespaces, master.getStorage()));
		environment.register(new ResultCSVResource(namespaces, master.getConfig()));
		environment.register(new StoredQueriesResource(namespaces));
		environment.register(IdParamConverter.Provider.INSTANCE);
		environment.register(CORSResponseFilter.class);
		environment.register(new ConfigResource(master.getConfig()));
	}
}
