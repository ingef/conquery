package com.bakdata.conquery.apiv1;

import java.time.Duration;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.result.arrow.ResultArrowProcessor;
import com.bakdata.conquery.io.result.csv.ResultCsvProcessor;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ActiveUsersFilter;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.api.*;
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
				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
				bind(manager.getStorage()).to(MetaStorage.class);

				bind(new ConceptsProcessor(manager.getDatasetRegistry())).to(ConceptsProcessor.class);
				bind(new MeProcessor(manager.getStorage(), datasets)).to(MeProcessor.class);
				bind(new QueryProcessor(datasets, manager.getStorage(), manager.getConfig())).to(QueryProcessor.class);
				bind(new FormConfigProcessor(manager.getValidator(), manager.getStorage())).to(FormConfigProcessor.class);
				bind(new StoredQueriesProcessor(manager.getDatasetRegistry(), manager.getStorage(), manager.getConfig())).to(StoredQueriesProcessor.class);
				bind(new ResultCsvProcessor(manager.getDatasetRegistry(), manager.getConfig())).to(ResultCsvProcessor.class);
				bind(new ResultArrowProcessor(manager.getDatasetRegistry(), manager.getConfig())).to(ResultArrowProcessor.class);
				bind(new ResultExcelProcessor(manager.getDatasetRegistry(), manager.getConfig())).to(ResultExcelProcessor.class);
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
		environment.register(ResultCsvResource.class);
		environment.register(ResultArrowFileResource.class);
		environment.register(ResultArrowStreamResource.class);
		environment.register(ResultExcelResource.class);
		environment.register(StoredQueriesResource.class);
		environment.register(IdParamConverter.Provider.INSTANCE);
		environment.register(new ConfigResource(manager.getConfig()));
		environment.register(FormConfigResource.class);

		environment.register(APIResource.class);
		environment.register(ConceptResource.class);
		environment.register(DatasetResource.class);
		environment.register(FilterResource.class);
		environment.register(MeResource.class);
	}
}
