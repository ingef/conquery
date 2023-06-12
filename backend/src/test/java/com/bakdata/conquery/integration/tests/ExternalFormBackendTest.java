package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.config.auth.ApiKeyClientFilterProvider;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.api.ResultExternalResource;
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

		final StandaloneSupport support = testConquery.getSupport(name);

		log.info("Test health");
		assertThat(testConquery.getStandaloneCommand()
							   .getManager()
							   .getEnvironment()
							   .healthChecks()
							   .runHealthCheck(FORM_BACKEND_ID)
							   .isHealthy())
				.describedAs("Checking health of form backend").isTrue();

		log.info("Get external form configs");
		final FormScanner formScanner = testConquery.getStandaloneCommand().getManager().getFormScanner();
		formScanner.execute(Collections.emptyMap(), null);

		final String externalFormId = FormBackendConfig.createSubTypedId("SOME_EXTERNAL_FORM");
		assertThat(FormScanner.FRONTEND_FORM_CONFIGS.keySet()).contains(externalFormId);

		log.info("Send an external form");
		final User testUser = support.getTestUser();
		final ManagedExecutionId
				managedExecutionId =
				IntegrationUtils.assertQueryResult(support, String.format("{\"type\": \"%s\", \"testProp\": \"testVal\"}", externalFormId), -1, ExecutionState.DONE, testUser, 201);

		log.info("Request state");
		final FullExecutionStatus executionStatus = IntegrationUtils.getExecutionStatus(support, managedExecutionId, testUser, 200);


		// Generate asset urls and check them in the status
		final UriBuilder apiUriBuilder = testConquery.getSupport(name).defaultApiURIBuilder();
		final ManagedExecution storedExecution = testConquery.getSupport(name).getMetaStorage().getExecution(managedExecutionId);
		final URI
				downloadURLasset1 =
				ResultExternalResource.getDownloadURL(apiUriBuilder.clone(), (ManagedExecution & ExternalResult) storedExecution, executionStatus.getResultUrls()
																																				 .get(0)
																																				 .getAssetId());
		final URI
				downloadURLasset2 =
				ResultExternalResource.getDownloadURL(apiUriBuilder.clone(), (ManagedExecution & ExternalResult) storedExecution, executionStatus.getResultUrls()
																																				 .get(1)
																																				 .getAssetId());


		assertThat(executionStatus.getStatus()).isEqualTo(ExecutionState.DONE);
		assertThat(executionStatus.getResultUrls()).containsExactly(new ResultAsset("Result", downloadURLasset1), new ResultAsset("Another Result", downloadURLasset2));

		log.info("Download Result");
		final String
				response =
				support.getClient().target(executionStatus.getResultUrls().get(0).url()).request(javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE).get(String.class);

		assertThat(response).isEqualTo("Hello");

		log.info("Stopping mock form backend server");
		formBackend.stop();
	}

	@Override
	@SneakyThrows(IOException.class)
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
	private URI createFormServer() throws IOException {
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
