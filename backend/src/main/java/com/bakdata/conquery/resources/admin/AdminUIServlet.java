package com.bakdata.conquery.resources.admin;

import java.util.ServiceLoader;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.commands.RESTServer;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.DefaultAuthFilter;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import lombok.Getter;

@Getter
public class AdminUIServlet {
	
	private DatasetsProcessor datasetsProcessor;

	public void register(MasterCommand masterCommand) {
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");
		
		RESTServer.configure(masterCommand.getConfig(), jerseyConfig);
		
		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		masterCommand.getEnvironment().admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");
		
		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));
		
		//jerseyConfig.getSingletons().add(new UnitOfWorkResourceMethodDispatchAdapter(hibernateBundle.getSessionFactory()));
		jerseyConfig.register(new AdminUIResource(masterCommand.getConfig(), masterCommand.getNamespaces(), masterCommand.getJobManager()));
		DatasetsResource datasets = new DatasetsResource(masterCommand.getConfig(), masterCommand.getStorage(), masterCommand.getNamespaces(), masterCommand.getJobManager());
		datasetsProcessor = datasets.getProcessor();
		jerseyConfig.register(datasets);
		jerseyConfig.register(new JobsResource(masterCommand.getJobManager()));
		jerseyConfig.register(new MultiPartFeature());
		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), ServiceLoader.load(ViewRenderer.class)));

		jerseyConfig.register(DefaultAuthFilter.asDropwizardFeature(masterCommand.getStorage(), masterCommand.getConfig().getAuthentication()));
		jerseyConfig.register(new AuthValueFactoryProvider.Binder<>(User.class));
		
		jerseyConfig.register(IdParamConverter.Provider.INSTANCE);
	}
}
