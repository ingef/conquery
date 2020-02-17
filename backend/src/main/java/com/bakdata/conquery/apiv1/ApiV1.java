package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.api.APIResource;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.api.FilterResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@CPSType(base = ResourcesProvider.class, id = "ApiV1")
public class ApiV1 implements ResourcesProvider {

	@Override
	public void registerResources(MasterCommand master) {
		Namespaces namespaces = master.getNamespaces();
		JerseyEnvironment environment = master.getEnvironment().jersey();

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(new ConceptsProcessor(master.getNamespaces())).to(ConceptsProcessor.class);
				bind(new MeProcessor(master.getStorage())).to(MeProcessor.class);
				bind(new QueryProcessor(namespaces,master.getStorage(), master.getMetricRegistry())).to(QueryProcessor.class);
			}
		});
		
		environment.register(new CORSPreflightRequestFilter());
		/*
		 * Register the authentication filter which protects all resources registered in this servlet.
		 * We use the same instance of the filter for the api servlet and the admin servlet to have a single 
		 * point for authentication.
		 */
		environment.register(master.getAuthController().getAuthenticationFilter());
		environment.register(QueryResource.class);
		environment.register(new ResultCSVResource(namespaces, master.getConfig()));
		environment.register(new StoredQueriesResource(namespaces));
		environment.register(IdParamConverter.Provider.INSTANCE);
		environment.register(CORSResponseFilter.class);
		environment.register(new ConfigResource(master.getConfig()));
		
		environment.register(APIResource.class);
		environment.register(ConceptResource.class);
		environment.register(DatasetResource.class);
		environment.register(FilterResource.class);
		environment.register(MeResource.class);
	}
}
