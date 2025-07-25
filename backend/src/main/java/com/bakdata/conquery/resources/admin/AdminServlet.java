package com.bakdata.conquery.resources.admin;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import jakarta.servlet.ServletRegistration;
import jakarta.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jackson.serializer.DatasetParamInjector;
import com.bakdata.conquery.io.jersey.IdPathParamConverterProvider;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenCheckFilter;
import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.admin.rest.AdminConceptsResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetsResource;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.admin.rest.AuthOverviewResource;
import com.bakdata.conquery.resources.admin.rest.ConfigApiResource;
import com.bakdata.conquery.resources.admin.rest.GroupResource;
import com.bakdata.conquery.resources.admin.rest.PermissionResource;
import com.bakdata.conquery.resources.admin.rest.RoleResource;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.rest.UserResource;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.AuthOverviewUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.GroupUIResource;
import com.bakdata.conquery.resources.admin.ui.IndexServiceUIResource;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;
import com.bakdata.conquery.resources.admin.ui.UserUIResource;
import com.bakdata.conquery.resources.admin.ui.model.ConnectorUIResource;
import io.dropwizard.core.setup.AdminEnvironment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.views.common.ViewMessageBodyWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class AdminServlet {

	public static final String ADMIN_UI = "admin-ui";
	private final AdminProcessor adminProcessor;
	private final DropwizardResourceConfig jerseyConfig;
	private final AdminDatasetProcessor adminDatasetProcessor;
	private final DropwizardResourceConfig jerseyConfigUI;

	public AdminServlet(ManagerNode manager) {
		jerseyConfig = new DropwizardResourceConfig(manager.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");
		jerseyConfigUI = new DropwizardResourceConfig(manager.getEnvironment().metrics());
		jerseyConfigUI.setUrlPattern("/admin-ui");

		RESTServer.configure(manager.getConfig(), jerseyConfig);
		RESTServer.configure(manager.getConfig(), jerseyConfigUI);

		final AdminEnvironment admin = manager.getEnvironment().admin();

		ServletRegistration.Dynamic adminServlet = admin.addServlet(ADMIN_SERVLET_PATH, new ServletContainer(jerseyConfig));
		adminServlet.addMapping("/" + ADMIN_SERVLET_PATH + "/*");
		admin.addServlet(ADMIN_UI_SERVLET_PATH, new ServletContainer(jerseyConfigUI)).addMapping("/" + ADMIN_UI_SERVLET_PATH + "/*");
		// Register static asset servlet for admin end
		admin.addServlet(ADMIN_ASSETS_PATH, new AssetServlet(ADMIN_ASSETS_PATH, "/" + ADMIN_ASSETS_PATH, null, StandardCharsets.UTF_8))
			 .addMapping("/" + ADMIN_ASSETS_PATH + "/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(manager.getEnvironment().getObjectMapper()));

		adminProcessor = new AdminProcessor(
				manager,
				manager.getConfig(),
				manager.getMetaStorage(),
				manager.getDatasetRegistry(),
				manager.getJobManager(),
				manager.getMaintenanceService(),
				manager.getValidator(),
				manager.getNodeProvider()
		);

		adminDatasetProcessor = new AdminDatasetProcessor(
				manager.getConfig(),
				manager.getDatasetRegistry(),
				manager.getMetaStorage(),
				manager.getJobManager(),
				manager.getImportHandler(),
				manager.getStorageListener(),
				manager.getEnvironment()
		);

		jerseyConfig.register(new AbstractBinder() {
						@Override
						protected void configure() {
							bind(manager).to(ManagerNode.class);
							bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
							bind(manager.getMetaStorage()).to(MetaStorage.class);
							bind(manager.getValidator()).to(Validator.class);
							bind(manager.getJobManager()).to(JobManager.class);
							bind(manager.getConfig()).to(ConqueryConfig.class);
							bind(adminProcessor).to(AdminProcessor.class);
							bind(adminDatasetProcessor).to(AdminDatasetProcessor.class);
						}
					})
					.register(AdminPermissionFilter.class)
					.register(IdPathParamConverterProvider.class)
					.register(AuthCookieFilter.class)
					.register(CsrfTokenCheckFilter.class)
					.register(DatasetParamInjector.class);


		jerseyConfigUI.register(new ViewMessageBodyWriter(manager.getEnvironment().metrics(), Collections.singleton(Freemarker.HTML_RENDERER)))
					  .register(new AbstractBinder() {
						  @Override
						  protected void configure() {
							  bind(adminProcessor).to(AdminProcessor.class);
							  bindAsContract(UIProcessor.class);
							  bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
							  bind(manager.getMetaStorage()).to(MetaStorage.class);
							  bind(manager.getConfig()).to(ConqueryConfig.class);
						  }
					  })
					  .register(IdPathParamConverterProvider.class)
					  .register(AdminPermissionFilter.class)
					  .register(AuthCookieFilter.class)
					  .register(CsrfTokenSetFilter.class)
					  .register(DatasetParamInjector.class);

	}

	public void register() {

		// register root resources
		jerseyConfig
				.register(AdminDatasetResource.class)
				.register(AdminDatasetsResource.class)
				.register(AdminConceptsResource.class)
				.register(AdminTablesResource.class)
				.register(RoleResource.class)
				.register(UserResource.class)
				.register(GroupResource.class)
				.register(PermissionResource.class)
				.register(AuthOverviewResource.class)
				.register(AdminResource.class)
				.register(ConfigApiResource.class);

		jerseyConfigUI
				.register(AdminUIResource.class)
				.register(RoleUIResource.class)
				.register(UserUIResource.class)
				.register(GroupUIResource.class)
				.register(DatasetsUIResource.class)
				.register(TablesUIResource.class)
				.register(ConceptsUIResource.class)
				.register(ConnectorUIResource.class)
				.register(AuthOverviewUIResource.class)
				.register(IndexServiceUIResource.class);

	}
}
