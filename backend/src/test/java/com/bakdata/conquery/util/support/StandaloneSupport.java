package com.bakdata.conquery.util.support;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StandaloneSupport implements TestSupport {

	public enum Mode {WORKER, SQL}

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
	private final AdminProcessor metaProcessor;
	@Getter
	private final AdminDatasetProcessor datasetsProcessor;
	@Getter
	private final User testUser;
	@Getter
	private final TestDataImporter testImporter;

	public AuthorizationController getAuthorizationController() {
		return testConquery.getStandaloneCommand().getManagerNode().getAuthController();
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



	public NamespaceStorage getNamespaceStorage() {
		return getStandaloneCommand().getManagerNode().getDatasetRegistry().get(dataset.getId()).getStorage();
	}

	public Client getClient() {
		return testConquery.getClient()
						   .register(new ConqueryAuthenticationFilter(getAuthorizationController().getConqueryTokenRealm().createTokenForUser(getTestUser().getId())));
	}

	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<ID>> VALUE resolve(ID id) {
		return (VALUE) getDatasetRegistry().get(id);
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

}
