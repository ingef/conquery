package com.bakdata.conquery.resources.admin;

import java.util.Collections;
import java.util.ServiceLoader;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.AuthCookieFilter;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.admin.rest.AdminConceptsResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;
import com.bakdata.conquery.resources.api.PermissionResource;
import com.bakdata.conquery.resources.api.RoleResource;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
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
		//freemarker support
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
		freemarker.configure(Freemarker.asMap());
		jerseyConfig.register(
			new ViewMessageBodyWriter(
				masterCommand.getEnvironment().metrics(),
				Collections.singleton(freemarker)
			)
		);

		adminProcessor = new AdminProcessor(
			masterCommand.getConfig(),
			masterCommand.getStorage(),
			masterCommand.getNamespaces(),
			masterCommand.getJobManager(),
			masterCommand.getMaintenanceService(),
			masterCommand.getValidator()
		);
		
		
		//inject required services
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(adminProcessor).to(AdminProcessor.class);
			}
		});
		
		//register root resources
		jerseyConfig
			.register(AdminResource.class)
			.register(AdminDatasetResource.class)
			.register(AdminConceptsResource.class)
			.register(AdminUIResource.class)
			.register(RoleResource.class)
			.register(RoleUIResource.class)
			.register(DatasetsUIResource.class)		
			.register(TablesUIResource.class)
			.register(ConceptsUIResource.class)
			.register(PermissionResource.class);
		
		//register features
		jerseyConfig
			.register(new MultiPartFeature())
			.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), ServiceLoader.load(ViewRenderer.class)))
			.register(masterCommand.getAuthDynamicFeature())
			.register(IdParamConverter.Provider.INSTANCE)
			.register(CORSResponseFilter.class)
			.register(AuthCookieFilter.class);
	}
}
