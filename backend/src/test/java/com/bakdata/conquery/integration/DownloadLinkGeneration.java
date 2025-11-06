package com.bakdata.conquery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadLinkGeneration extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final MetaStorage storage = conquery.getMetaStorage();

		final User user = new User("testU", "testU", storage);

		final QueryTest test = (QueryTest) ConqueryTestSpec.fromResourcePath("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json");

		storage.updateUser(user);

		// Manually import data
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		test.importRequiredData(conquery);

		// Parse the query in the context of the conquery instance, not the test, to have the IdResolver properly set
		Query query = LoadingUtil.parseSubTree(conquery, test.getRawQuery(), Query.class, true);

		// Create execution for download
		ManagedQuery exec = new ManagedQuery(query, user.getId(), conquery.getDataset(), storage, conquery.getDatasetRegistry(), conquery.getConfig());
		exec.setLastResultCount(100L);

		storage.addExecution(exec);

		user.addPermission(DatasetPermission.onInstance(Set.of(Ability.READ), conquery.getDataset()));

		{
			// Try to generate a download link: should not be possible, because the execution isn't run yet
			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			assertThat(status.getResultUrls()).isEmpty();
		}

		{
			// Tinker the state of the execution and try again: still not possible because of missing permissions
			DistributedExecutionManager.DistributedExecutionInfo distributedState = new DistributedExecutionManager.DistributedExecutionInfo(Collections.emptyList());
			distributedState.setExecutionState(ExecutionState.DONE);
			distributedState.getExecutingLock().countDown();
			conquery.getNamespace().getExecutionManager().addState(exec.getId(), distributedState);

			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			assertThat(status.getResultUrls()).isEmpty();
		}

		{
			// Add permission to download: now it should be possible
			user.addPermission(DatasetPermission.onInstance(Set.of(Ability.DOWNLOAD), conquery.getDataset()));

			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, exec.getId(), user, 200);
			// This Url is missing the `/api` path part, because we use the standard UriBuilder here
			assertThat(status.getResultUrls()).contains(new ResultAsset("CSV", new URI(String.format("%s/result/csv/%s.csv", conquery.defaultApiURIBuilder()
																																	 .toString(), exec.getId()))));
		}
	}

}
