package com.bakdata.conquery.models.auth.web;

import java.util.Collections;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.AuthorizationController;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import org.apache.shiro.realm.Realm;
import org.glassfish.jersey.servlet.ServletContainer;

public class AuthServlet {

	/**
	 * Marker interface for classes that provide admin UI functionality. Classes
	 * have to register as CPSType=AdminServletResource and will then be able to be
	 * registered in the admin jerseyconfig.
	 */
	@CPSBase
	public interface AuthResourceProvider {
		void registerResources(DropwizardResourceConfig jerseyConfig);
	}
	
	public void register(MasterCommand masterCommand, AuthorizationController controller) {
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/auth");

		RESTServer.configure(masterCommand.getConfig(), jerseyConfig);

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		masterCommand.getEnvironment().servlets().addServlet("auth", servletContainerHolder.getContainer()).addMapping("/auth/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));
		// freemarker support
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
		freemarker.configure(Freemarker.asMap());
		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), Collections.singleton(freemarker)));
		

		// Scan realms if they need to add resources
		for (Realm realm : controller.getRealms()) {
			if(realm instanceof AuthResourceProvider) {
				((AuthResourceProvider)realm).registerResources(jerseyConfig);
			}
		}
	}
}
