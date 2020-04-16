package com.bakdata.conquery.resources.admin;

import java.util.Collections;
import java.util.ServiceLoader;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.bakdata.conquery.resources.admin.rest.AdminConceptsResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.admin.rest.AuthOverviewResource;
import com.bakdata.conquery.resources.admin.rest.GroupResource;
import com.bakdata.conquery.resources.admin.rest.PermissionResource;
import com.bakdata.conquery.resources.admin.rest.RoleResource;
import com.bakdata.conquery.resources.admin.rest.UserResource;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.AuthOverviewUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.GroupUIResource;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;
import com.bakdata.conquery.resources.admin.ui.UserUIResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.realm.Realm;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class AdminServlet {

	/**
	 * Marker interface for classes that provide admin UI functionality.
	 */
	@CPSBase
	public interface AuthAdminResourceProvider {
		void registerAuthenticationAdminResources(DropwizardResourceConfig jerseyConfig);
	}

	private AdminProcessor adminProcessor;
	private DropwizardResourceConfig jerseyConfig;

	public void register(MasterCommand masterCommand, AuthorizationController controller) {
		jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");

		RESTServer.configure(masterCommand.getConfig(), jerseyConfig);

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		masterCommand.getEnvironment().admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));
		// freemarker support
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
		freemarker.configure(Freemarker.asMap());
		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), Collections.singleton(freemarker)));

		adminProcessor = new AdminProcessor(
			masterCommand.getConfig(),
			masterCommand.getStorage(),
			masterCommand.getNamespaces(),
			masterCommand.getJobManager(),
			masterCommand.getMaintenanceService(),
			masterCommand.getValidator());

		// inject required services
		jerseyConfig.register(new AbstractBinder() {

			@Override
			protected void configure() {
				bind(adminProcessor).to(AdminProcessor.class);
			}
		});

		// register root resources
		jerseyConfig
			.register(AdminResource.class)
			.register(AdminDatasetResource.class)
			.register(AdminConceptsResource.class)
			.register(AdminTablesResource.class)
			.register(AdminUIResource.class)
			.register(RoleResource.class)
			.register(RoleUIResource.class)
			.register(UserResource.class)
			.register(UserUIResource.class)
			.register(GroupResource.class)
			.register(GroupUIResource.class)
			.register(DatasetsUIResource.class)
			.register(TablesUIResource.class)
			.register(ConceptsUIResource.class)
			.register(PermissionResource.class)
			.register(AuthOverviewUIResource.class)
			.register(AuthOverviewResource.class);

		// Scan calsspath for Admin side plugins and register them.
		for ( Realm realm : controller.getRealms()) {
			if(realm instanceof AuthAdminResourceProvider) {
				((AuthAdminResourceProvider)realm).registerAuthenticationAdminResources(jerseyConfig);
			}
		}

		// register features
		jerseyConfig
			.register(new MultiPartFeature())
			.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), ServiceLoader.load(ViewRenderer.class)))
			.register(new CORSPreflightRequestFilter())
			.register(masterCommand.getAuthController().getAuthenticationFilter())
			.register(IdParamConverter.Provider.INSTANCE)
			.register(CORSResponseFilter.class)
			.register(AuthCookieFilter.class);
	}
}
