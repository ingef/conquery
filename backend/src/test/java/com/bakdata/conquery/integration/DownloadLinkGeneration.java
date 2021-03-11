package com.bakdata.conquery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadLinkGeneration extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static final String BASE = "http://localhost";

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final User user = new User("testU", "testU");

		final String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8()
			.readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(conquery.getDataset(), testJson);

		conquery.getMetaStorage().updateUser(user);

		// Manually import data
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		test.importRequiredData(conquery);

		// Create execution for download
		ManagedQuery exec = new ManagedQuery(test.getQuery(), user.getId(), conquery.getDataset().getId());

		DatasetRegistry datasetRegistry = conquery.getDatasetsProcessor().getDatasetRegistry();
		{			
			// Try to generate a download link: should not be possible, because the execution isn't run yet
			Optional<URL> url = exec.getDownloadURL(UriBuilder.fromUri(URI.create(BASE)), user, AuthorizationHelper.buildDatasetAbilityMap(user,datasetRegistry));
			assertThat(url).isEmpty();
		}

		{			
			// Thinker the state of the execution and try again: still not possible because of missing permissions
			exec.setState(ExecutionState.DONE);
			
			Optional<URL> url = exec.getDownloadURL(UriBuilder.fromUri(URI.create(BASE)), user, AuthorizationHelper.buildDatasetAbilityMap(user,datasetRegistry));
			assertThat(url).isEmpty();
		}

		{			
			// Add permissions: now it should be possible
			user.addPermission(
				conquery.getMetaStorage(),
				DatasetPermission.onInstance(Set.of(Ability.READ, Ability.DOWNLOAD), conquery.getDataset().getId()));
			
			Optional<URL> url = exec.getDownloadURL(UriBuilder.fromUri(URI.create(BASE)), user, AuthorizationHelper.buildDatasetAbilityMap(user,datasetRegistry));
			// This Url is missing the `/api` path part, because we use the standard UriBuilder here
			assertThat(url).contains(new URL(String.format("%s/datasets/%s/result/%s.csv", BASE, conquery.getDataset().getId(), exec.getId())));
		}
	}

}
