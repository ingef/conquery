package com.bakdata.conquery.util.support;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.setup.Environment;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StandaloneSupport {

	private final TestConquery testConquery;
	@Getter
	private final DistributedNamespace namespace;
	@Getter
	private final Dataset dataset;
	@Getter
	private final File tmpDir;
	@Getter
	private final ConqueryConfig config;
	@Getter
	private final AdminProcessor metaProcessor;
	@Getter
	private final AdminDatasetProcessor<DistributedNamespace> datasetsProcessor;
	@Getter
	private final User testUser;

	public AuthorizationController getAuthorizationController() {
		return testConquery.getStandaloneCommand().getManager().getAuthController();
	}

	public void waitUntilWorkDone() {
		testConquery.waitUntilWorkDone();
	}

	public void preprocessTmp(File tmpDir, List<File> descriptions) throws Exception {
		final Environment env = testConquery.getDropwizard().getEnvironment();
		final net.sourceforge.argparse4j.inf.Namespace namespace = new net.sourceforge.argparse4j.inf.Namespace(
				Map.of(
						"in", tmpDir,
						"out", tmpDir,
						"desc", descriptions

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


	public Validator getValidator() {
		return testConquery.getStandaloneCommand().getManager().getValidator();
	}

	public MetaStorage getMetaStorage() {
		return testConquery.getStandaloneCommand().getManager().getStorage();
	}

	public NamespaceStorage getNamespaceStorage() {
		return testConquery.getStandaloneCommand().getManager().getDatasetRegistry().get(dataset.getId()).getStorage();
	}

	public DatasetRegistry getDatasetRegistry() {
		return testConquery.getStandaloneCommand().getManager().getDatasetRegistry();
	}

	public List<ShardNode> getShardNodes() {
		return testConquery.getStandaloneCommand().getShardNodes();
	}

	/**
	 * Retrieves the port of the admin API.
	 *
	 * @return The port.
	 */
	public int getAdminPort() {
		return testConquery.getDropwizard().getAdminPort();
	}

	public Client getClient() {
		return testConquery.getClient()
						   .register(new ConqueryAuthenticationFilter(getAuthorizationController().getConqueryTokenRealm().createTokenForUser(getTestUser().getId())));
	}

	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<ID>> VALUE resolve(ID id) {
		return getDatasetRegistry().resolve(id);
	}

	@Data
	private static class ConqueryAuthenticationFilter implements ClientRequestFilter {
		private final String token;

		@Override
		public void filter(ClientRequestContext requestContext) throws IOException {
			// If none set to provided token
			if(requestContext.getHeaders().containsKey("Authorization")){
				return;
			}

			requestContext.getHeaders().add("Authorization", "Bearer " + getToken());
		}
	}

	/**
	 * Retrieves the port of the main API.
	 *
	 * @return The port.
	 */
	public int getLocalPort() {
		return testConquery.getDropwizard().getLocalPort();
	}

	public UriBuilder defaultApiURIBuilder() {
		return UriBuilder.fromPath("api")
						 .host("localhost")
						 .scheme("http")
						 .port(getLocalPort());
	}

	public UriBuilder defaultAdminURIBuilder() {
		return UriBuilder.fromPath("admin")
						 .host("localhost")
						 .scheme("http")
						 .port(getAdminPort());
	}
}
