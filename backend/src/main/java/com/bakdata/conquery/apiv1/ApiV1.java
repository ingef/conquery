package com.bakdata.conquery.apiv1;

import java.time.Duration;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.IdRefPathParamConverterProvider;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ActiveUsersFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.api.APIResource;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConfigResource;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.api.FilterResource;
import com.bakdata.conquery.resources.api.FormConfigResource;
import com.bakdata.conquery.resources.api.MeResource;
import com.bakdata.conquery.resources.api.QueryResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@CPSType(base = ResourcesProvider.class, id = "ApiV1")
public class ApiV1 implements ResourcesProvider {

	@Override
	public void registerResources(ManagerNode manager) {
		DatasetRegistry datasets = manager.getDatasetRegistry();
		JerseyEnvironment environment = manager.getEnvironment().jersey();

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(manager.getConfig()).to(ConqueryConfig.class);

				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
				bind(manager.getStorage()).to(MetaStorage.class);

				bind(new ConceptsProcessor(manager.getDatasetRegistry())).to(ConceptsProcessor.class);
				bind(new MeProcessor(manager.getStorage(), datasets)).to(MeProcessor.class);
				bind(new QueryProcessor(datasets, manager.getStorage(), manager.getConfig())).to(QueryProcessor.class);
				bind(new FormConfigProcessor(manager.getValidator(), manager.getStorage())).to(FormConfigProcessor.class);
			}
		});

		environment.register(CORSPreflightRequestFilter.class);
		environment.register(CORSResponseFilter.class);

		environment.register(
				new ActiveUsersFilter(
						manager.getStorage(),
						Duration.ofMinutes(manager.getConfig().getMetricsConfig().getUserActiveDuration().toMinutes())
				)
		);

		/*
		 * Register the authentication filter which protects all resources registered in this servlet.
		 * We use the same instance of the filter for the api servlet and the admin servlet to have a single
		 * point for authentication.
		 */
		environment.register(manager.getAuthController().getAuthenticationFilter());
		environment.register(QueryResource.class);
		environment.register(IdParamConverter.Provider.INSTANCE);
		environment.register(new ConfigResource(manager.getConfig()));
		environment.register(FormConfigResource.class);

		environment.register(APIResource.class);
		environment.register(ConceptResource.class);
		environment.register(DatasetResource.class);
		environment.register(FilterResource.class);
		environment.register(MeResource.class);

		for (ResultRendererProvider resultProvider : manager.getConfig().getResultProviders()) {
			resultProvider.registerResultResource(environment,manager);
		}

		environment.register(new IdRefPathParamConverterProvider(manager.getDatasetRegistry(), manager.getDatasetRegistry().getMetaRegistry()));
	}
}
