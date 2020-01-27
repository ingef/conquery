package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.AuthorizationStorage;
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
		AuthorizationStorage authStorage = AuthorizationController.getInstance().getAuthStorage();

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(new ConceptsProcessor(master.getNamespaces())).to(ConceptsProcessor.class);
				bind(new QueryProcessor(namespaces, authStorage)).to(QueryProcessor.class);
				bind(new MeProcessor(authStorage)).to(MeProcessor.class);
			}
		});
		
		environment.register(new CORSPreflightRequestFilter());
		environment.register(master.getAuthDynamicFeature());
		environment.register(QueryResource.class);
		environment.register(new ResultCSVResource(namespaces, master.getConfig()));
		environment.register(new StoredQueriesResource(namespaces, authStorage));
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
