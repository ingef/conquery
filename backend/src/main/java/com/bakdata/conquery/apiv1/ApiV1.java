package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.IdRefPathParamConverterProvider;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.metrics.ActiveUsersFilter;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormProcessor;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConfigResource;
import com.bakdata.conquery.resources.api.DatasetFormConfigResource;
import com.bakdata.conquery.resources.api.DatasetFormResource;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.api.DatasetsResource;
import com.bakdata.conquery.resources.api.FilterResource;
import com.bakdata.conquery.resources.api.FormConfigResource;
import com.bakdata.conquery.resources.api.MeResource;
import com.bakdata.conquery.resources.api.QueryResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@CPSType(base = ResourcesProvider.class, id = "ApiV1")
public class ApiV1 implements ResourcesProvider {

	@Override
	public void registerResources(ManagerNode manager) {

		JerseyEnvironment jersey = manager.getEnvironment().jersey();
		// TODO this does not work, if we really want to do api versioning
		jersey.setUrlPattern("/api");

		// Inject Processors
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(QueryProcessor.class);
				bindAsContract(ConceptsProcessor.class);
				bindAsContract(MeProcessor.class);
				bindAsContract(FormConfigProcessor.class);
				bindAsContract(FormProcessor.class);
			}
		});

		jersey.register(CORSPreflightRequestFilter.class);
		jersey.register(CORSResponseFilter.class);
		jersey.register(IdRefPathParamConverterProvider.class);

		jersey.register(ActiveUsersFilter.class);


		/*
		 * Register the authentication filter which protects all resources registered in this servlet.
		 * We use the same instance of the filter for the api servlet and the admin servlet to have a single
		 * point for authentication.
		 */
		jersey.register(manager.getAuthController().getAuthenticationFilter());
		jersey.register(IdParamConverter.Provider.INSTANCE);

		jersey.register(QueryResource.class);
		jersey.register(DatasetQueryResource.class);
		jersey.register(DatasetFormConfigResource.class);
		jersey.register(ConfigResource.class);
		jersey.register(FormConfigResource.class);
		jersey.register(DatasetFormResource.class);

		jersey.register(DatasetsResource.class);
		jersey.register(ConceptResource.class);
		jersey.register(DatasetResource.class);
		jersey.register(FilterResource.class);
		jersey.register(MeResource.class);
	}
}
