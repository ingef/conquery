package com.bakdata.conquery.util.support;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StandaloneSupport implements TestSupport {

	@Getter
	private final Mode mode;
	@Delegate
	private final TestConquery testConquery;
	@Getter
	private final Namespace namespace;
	@Getter
	private final Dataset dataset;
	@Getter
	private final File tmpDir;
	@Getter
	private final ConqueryConfig config;
	@Getter
	private final TestDataImporter testImporter;

	public void waitUntilWorkDone() {
		testConquery.waitUntilWorkDone();
	}

	public void preprocessTmp(File tmpDir, List<File> descriptions) throws Exception {
		final Environment env = testConquery.getDropwizard().getEnvironment();
		final net.sourceforge.argparse4j.inf.Namespace namespace = new net.sourceforge.argparse4j.inf.Namespace(
				Map.of(
						"in", tmpDir,
						"out", tmpDir,
						"desc", descriptions,
						"buckets", 10,
						"strict", true,
						"fast-fail", true
				)
		);

		// We use this to change the visibility of the run method, hence it cannot be instantiated.
		new PreprocessorCommand(MoreExecutors.newDirectExecutorService()){
			@Override
			public void run(Environment environment, net.sourceforge.argparse4j.inf.Namespace namespace, ConqueryConfig config) throws Exception {
				super.run(environment, namespace, config);
			}
		}
		.run(env, namespace, config);
	}

	public NamespaceStorage getNamespaceStorage() {
		return getStandaloneCommand().getManagerNode().getDatasetRegistry().get(dataset.getId()).getStorage();
	}

	public AuthorizationController getAuthorizationController() {
		return testConquery.getStandaloneCommand().getManagerNode().getAuthController();
	}

	/**
	 * Returns a http client with registered authentication.
	 * The user is by default the initial user with a super permission.
	 *
	 * @return The http client
	 */
	public Client getClient() {
		return testConquery.getClient();
	}

	public UriBuilder defaultApiURIBuilder() {
		return UriBuilder.fromPath("api")
						 .host("localhost")
						 .scheme("http")
						 .port(getLocalPort());
	}

	/**
	 * Retrieves the port of the main API.
	 *
	 * @return The port.
	 */
	public int getLocalPort() {
		return testConquery.getDropwizard().getLocalPort();
	}

	public UriBuilder defaultAdminURIBuilder() {
		return UriBuilder.fromPath("admin")
						 .host("localhost")
						 .scheme("http")
						 .port(getAdminPort());
	}

	/**
	 * Retrieves the port of the admin API.
	 *
	 * @return The port.
	 */
	public int getAdminPort() {
		return testConquery.getDropwizard().getAdminPort();
	}

	public enum Mode {WORKER, SQL}


}
