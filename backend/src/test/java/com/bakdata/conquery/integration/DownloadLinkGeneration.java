package com.bakdata.conquery.integration;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Slf4j
public class DownloadLinkGeneration extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final MetaStorage storage = conquery.getMetaStorage();

		final User user = new User("testU", "testU");
		user.setMetaStorage(storage);

		final String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(conquery.getDataset(), testJson);

		storage.updateUser(user);

		// Manually import data
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		test.importRequiredData(conquery);


		user.addPermission(DatasetPermission.onInstance(Set.of(Ability.READ), conquery.getDataset().getId()));
		conquery.getNamespace().getStorage().getAllConcepts().forEach( concept ->
			user.addPermission(concept.createPermission(Ability.READ.asSet()))
		);

		final ManagedExecutionId executionId = IntegrationUtils.assertQueryResult(conquery, test.getQuery(), -1, ExecutionState.DONE, user, 201);

		if (executionId == null) {
			fail("Execution id should not be null");
		}

		{
			// Try to generate a download link: not possible because of missing permissions
			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, executionId, user, 200);
			assertThat(status.getResultUrls()).isEmpty();
		}

		{
			// Add permission to download: now it should be possible
			user.addPermission(DatasetPermission.onInstance(Set.of(Ability.DOWNLOAD), conquery.getDataset().getId()));

			FullExecutionStatus status = IntegrationUtils.getExecutionStatus(conquery, executionId, user, 200);
			// This Url is missing the `/api` path part, because we use the standard UriBuilder here
			assertThat(status.getResultUrls()).contains(new ResultAsset("CSV", new URI(String.format("%s/result/csv/%s.csv", conquery.defaultApiURIBuilder()
																																	 .toString(), executionId))));
		}
	}

}
