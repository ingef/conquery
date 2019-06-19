package com.bakdata.eva.models.translation;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminUIServlet;
import com.bakdata.conquery.resources.admin.DatasetsResource;
import com.bakdata.eva.models.translation.query.QueryTranslator;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(base = ResourcesProvider.class, id = "translation")
public class TranslationAPI implements ResourcesProvider {

	@Override
	public void registerResources(MasterCommand masterCommand) {
		Namespaces namespaces = masterCommand.getNamespaces();
		JerseyEnvironment environment = masterCommand.getEnvironment().jersey();

		DatasetsResource datasets = new DatasetsResource(masterCommand.getConfig(), masterCommand.getStorage(), masterCommand.getNamespaces(), masterCommand.getJobManager(), masterCommand.getMaintenanceService());

		environment.register(MultiPartFeature.class);
		environment.register(new QueryTranslator(namespaces, masterCommand.getStorage(), datasets.getProcessor()));

	}
}
