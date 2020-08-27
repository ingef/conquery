package com.bakdata.conquery.util.support;

import java.io.Closeable;
import java.io.File;
import java.util.List;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.testing.DropwizardTestSupport;
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

	public void preprocessTmp() {
		DropwizardTestSupport<ConqueryConfig> prepro = new DropwizardTestSupport<>(
			Conquery.class,
			config,
			app -> new TestCommandWrapper(config, new PreprocessorCommand(MoreExecutors.newDirectExecutorService()))
		);
		prepro.before();
		prepro.after();
	}

	@Override
	public void close() {
		testConquery.closeNamespace(getDataset().getId());
	}

	public Validator getValidator() {
		return testConquery.getStandaloneCommand().getMaster().getValidator();
	}

	public MasterMetaStorage getMasterMetaStorage() {
		return testConquery.getStandaloneCommand().getMaster().getStorage();
	}

	public NamespaceStorage getNamespaceStorage() {
		return testConquery.getStandaloneCommand().getMaster().getNamespaces().get(dataset.getId()).getStorage();
	}

	public List<SlaveCommand> getSlaves() {
		return testConquery.getStandaloneCommand().getSlaves();
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
