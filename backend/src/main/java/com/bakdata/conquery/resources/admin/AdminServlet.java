package com.bakdata.conquery.resources.admin;

import java.util.Collections;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
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
import io.dropwizard.views.ViewMessageBodyWriter;
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

	public void register(ManagerNode manager) {
		jerseyConfig = new DropwizardResourceConfig(manager.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");

		RESTServer.configure(manager.getConfig(), jerseyConfig);

		manager.getEnvironment().admin().addServlet("admin", new ServletContainer(jerseyConfig)).addMapping("/admin/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(manager.getEnvironment().getObjectMapper()));
		// freemarker support
		jerseyConfig.register(new ViewMessageBodyWriter(manager.getEnvironment().metrics(), Collections.singleton(Freemarker.HTML_RENDERER)));

		adminProcessor = new AdminProcessor(
			manager.getConfig(),
			manager.getStorage(),
			manager.getDatasetRegistry(),
			manager.getJobManager(),
			manager.getMaintenanceService(),
			manager.getValidator());

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

		// Scan classpath for Admin side plugins and register them.
		for ( Realm realm : manager.getAuthController().getRealms()) {
			if(realm instanceof AuthAdminResourceProvider) {
				((AuthAdminResourceProvider)realm).registerAuthenticationAdminResources(jerseyConfig);
			}
		}

		// register features
		jerseyConfig
			.register(new MultiPartFeature())
			.register(manager.getAuthController().getAuthenticationFilter())
			.register(IdParamConverter.Provider.INSTANCE)
			.register(AuthCookieFilter.class);
	}
}
