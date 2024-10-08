package com.bakdata.conquery.integration.tests;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.frontend.FrontendConfiguration;
import com.bakdata.conquery.apiv1.frontend.VersionContainer;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.config.auth.ApiKeyClientFilterProvider;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.api.ConfigResource;
import com.bakdata.conquery.resources.api.ResultExternalResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.OpenAPIExpectation;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;

@Slf4j
public class ExternalFormBackendTest implements ProgrammaticIntegrationTest {

	public static final String FORM_BACKEND_ID = "mock";
	private ClientAndServer formBackend;

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		try {

			final StandaloneSupport support = testConquery.getSupport(name);

			log.info("Test health");
			assertThat(testConquery.getStandaloneCommand()
								   .getManagerNode()
								   .getEnvironment()
								   .healthChecks()
								   .runHealthCheck(FORM_BACKEND_ID)
								   .isHealthy())
					.describedAs("Checking health of form backend").isTrue();

			log.info("Get external form configs");
			final FormScanner formScanner = testConquery.getStandaloneCommand().getManagerNode().getFormScanner();
			formScanner.execute(Collections.emptyMap(), null);

			final String externalFormId = FormBackendConfig.createSubTypedId("SOME_EXTERNAL_FORM");
			assertThat(FormScanner.FRONTEND_FORM_CONFIGS.keySet()).contains(externalFormId);

			log.info("Get version info");
			final UriBuilder apiUriBuilder = testConquery.getSupport(name).defaultApiURIBuilder();
			final URI frontendConfigURI = HierarchyHelper.hierarchicalPath(apiUriBuilder.clone(), ConfigResource.class, "getFrontendConfig")
														 .build();
			final FrontendConfiguration
					frontendConfiguration =
					support.getClient().target(frontendConfigURI).request(MediaType.APPLICATION_JSON_TYPE).get().readEntity(FrontendConfiguration.class);

			assertThat(frontendConfiguration.versions())
					.describedAs("Checking health of form backend")
					.contains(new VersionContainer(FORM_BACKEND_ID, "3.2.1-ge966c285", ZonedDateTime.parse("2007-08-31T16:47:00+00:00"))); // example value from OpenAPI Spec

			log.info("Send an external form");
			final User testUser = support.getTestUser();
			final ManagedExecutionId
					managedExecutionId =
					IntegrationUtils.assertQueryResult(support, String.format("{\"type\": \"%s\", \"testProp\": \"testVal\"}", externalFormId), -1, ExecutionState.DONE, testUser, 201);

			log.info("Request state");
			assert managedExecutionId != null;
			final FullExecutionStatus executionStatus = IntegrationUtils.getExecutionStatus(support, managedExecutionId, testUser, 200);

		assertThat(executionStatus.getStatus()).isEqualTo(ExecutionState.DONE);

			// Generate asset urls and check them in the status
			final ManagedExecution storedExecution = testConquery.getSupport(name).getMetaStorage().getExecution(managedExecutionId);
			final URI
					downloadUrlAsset1 =
					ResultExternalResource.getDownloadURL(apiUriBuilder.clone(), (ExternalExecution) storedExecution, executionStatus.getResultUrls()
																																	 .get(0)
																																	 .getAssetId());
			final URI
					downloadUrlAsset2 =
					ResultExternalResource.getDownloadURL(apiUriBuilder.clone(), (ExternalExecution) storedExecution, executionStatus.getResultUrls()
																																	 .get(1)
																																	 .getAssetId());



			assertThat(executionStatus.getResultUrls()).containsExactly(new ResultAsset("Result", downloadUrlAsset1), new ResultAsset("Another Result", downloadUrlAsset2));

			log.info("Download Result");
			final String
					response =
					support.getClient().target(executionStatus.getResultUrls().get(0).url()).request(TEXT_PLAIN_TYPE).get(String.class);

			assertThat(response).isEqualTo("Hello");

			log.info("Stopping mock form backend server");
		} finally {
			formBackend.stop();
		}
	}

	@Override
	public ConqueryConfig overrideConfig(ConqueryConfig conf, File workdir) {
		// Prepare mock server
		final URI baseURI = createFormServer();

		// Setup external form backend in config
		final List<PluginConfig> plugins = conf.getPlugins();

		final FormBackendConfig externalConf = new FormBackendConfig();
		externalConf.setId(FORM_BACKEND_ID);
		externalConf.setBaseURI(baseURI);
		externalConf.setAuthentication(new ApiKeyClientFilterProvider("test-token"));

		plugins.add(externalConf);

		conf.setPlugins(plugins);


		// Create new storage path to prevent xodus lock conflicts
		XodusStoreFactory storageConfig = (XodusStoreFactory) conf.getStorage();
		final Path storageDir = workdir.toPath().resolve(storageConfig.getDirectory().resolve(getClass().getSimpleName()));

		return conf.withStorage(storageConfig.withDirectory(storageDir));
	}

	@SneakyThrows
	@NotNull
	private URI createFormServer() {
		log.info("Starting mock form backend server");
		formBackend = ClientAndServer.startClientAndServer(1080);

		final URI baseURI = URI.create(String.format("http://127.0.0.1:%d", formBackend.getPort()));

		Expectation[] expectations = formBackend.upsert(OpenAPIExpectation.openAPIExpectation("/com/bakdata/conquery/external/openapi-form-backend.yaml"));


		// Result request matcher
		formBackend.when(request("/result.txt")).respond(HttpResponse.response().withBody(StringBody.exact("Hello")));


		// Trap: Log failed request
		formBackend.when(request()).respond(httpRequest -> {
			log.error("{} on {}\n\t Headers: {}\n\tBody {}", httpRequest.getMethod(), httpRequest.getPath(), httpRequest.getHeaderList(), httpRequest.getBodyAsString());
			return HttpResponse.notFoundResponse();
		});
		return baseURI;
	}
}
