package com.bakdata.conquery.resources.admin;

import java.util.ServiceLoader;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.AuthCookieFilter;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import lombok.Getter;

@Getter
public class AdminServlet {

	private AdminProcessor adminProcessor;

	public void register(MasterCommand masterCommand) {
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");
		
		RESTServer.configure(masterCommand.getConfig(), jerseyConfig);
		
		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		masterCommand.getEnvironment().admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));

		adminProcessor = new AdminProcessor(
			masterCommand.getConfig(),
			masterCommand.getStorage(),
			masterCommand.getNamespaces(),
			masterCommand.getJobManager(),
			masterCommand.getMaintenanceService()
		);
		
		
		//inject required services
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(adminProcessor).to(AdminProcessor.class);
			}
		});
		
		//register root resources
		jerseyConfig.register(AdminResource.class);
		jerseyConfig.register(AdminDatasetResource.class);
		jerseyConfig.register(AdminUIResource.class);
		jerseyConfig.register(DatasetsUIResource.class);		
		jerseyConfig.register(TablesUIResource.class);
		jerseyConfig.register(ConceptsUIResource.class);
		
		//register features
		jerseyConfig.register(new MultiPartFeature());
		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), ServiceLoader.load(ViewRenderer.class)));
		jerseyConfig.register(masterCommand.getAuthDynamicFeature());
		jerseyConfig.register(IdParamConverter.Provider.INSTANCE);
		jerseyConfig.register(CORSResponseFilter.class);
		jerseyConfig.register(AuthCookieFilter.class);
	}
}
