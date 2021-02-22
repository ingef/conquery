package com.bakdata.conquery.util.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Optional;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.JarLocation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class StandaloneSupport implements Closeable {

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
	private final AdminProcessor datasetsProcessor;
	@Getter
	private final User testUser;

	public void waitUntilWorkDone() {
		testConquery.waitUntilWorkDone();
	}

	public void preprocessTmp(File tmpDir) throws Exception {
		// Setup necessary mock
		final JarLocation location = mock(JarLocation.class);
		when(location.getVersion()).thenReturn(Optional.of("1.0.0"));

		// Add commands you want to test
		final Bootstrap<ConqueryConfig> bootstrap = new Bootstrap<>(new Conquery());

		bootstrap.addCommand(new PreprocessorCommand(MoreExecutors.newDirectExecutorService()));

		final Cli cli = new Cli(location, bootstrap, System.out, System.err);

		cli.run("preprocess", "--in", tmpDir.toString(), "--desc", tmpDir.toString(), "--out", tmpDir.toString());
	}

	@Override
	public void close() {
		testConquery.removeSupportDataset(this);
		testConquery.removeSupport(this);
		testConquery.waitUntilWorkDone();
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

	public List<ShardNode> getShardNodes() {
		return testConquery.getStandaloneCommand().getShardNodes();
	}
	
	/**
	 * Retrieves the port of the admin API.
	 * @return The port.
	 */
	public int getAdminPort() {
		return testConquery.getDropwizard().getAdminPort();
	}

	public Client getClient() {
		return testConquery.getClient();
	}

	/**
	 * Retrieves the port of the main API.
	 * @return The port.
	 */
	public int getLocalPort() {
		return testConquery.getDropwizard().getLocalPort();
	}
}
