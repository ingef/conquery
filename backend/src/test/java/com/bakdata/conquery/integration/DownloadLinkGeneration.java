package com.bakdata.conquery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Set;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadLinkGeneration extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

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
		ManagedQuery exec = new ManagedQuery(test.getQuery(), user, conquery.getDataset());

		conquery.getMetaStorage().addExecution(exec);

		user.addPermission(
				conquery.getMetaStorage(),
				DatasetPermission.onInstance(Set.of(Ability.READ), conquery.getDataset().getId()));

		{
			// Try to generate a download link: should not be possible, because the execution isn't run yet
			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			assertThat(status.getResultUrls()).isEmpty();
		}

		{			
			// Thinker the state of the execution and try again: still not possible because of missing permissions
			exec.setState(ExecutionState.DONE);

			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			assertThat(status.getResultUrls()).isEmpty();
		}

		{			
			// Add permission to download: now it should be possible
			user.addPermission(
				conquery.getMetaStorage(),
				DatasetPermission.onInstance(Set.of(Ability.DOWNLOAD), conquery.getDataset().getId()));

			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			// This Url is missing the `/api` path part, because we use the standard UriBuilder here
			assertThat(status.getResultUrls()).contains(new URL(String.format("%s/datasets/%s/result/%s.csv", conquery.defaultApiURIBuilder().toString(), conquery.getDataset().getId(), exec.getId())));
		}
	}

}
