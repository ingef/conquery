package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConceptPermissionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final MetaStorage storage = conquery.getMetaStorage();
		final Dataset dataset = conquery.getDataset();
		final String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset.getId(), testJson);
		final QueryProcessor processor = new QueryProcessor(storage.getDatasetRegistry(), storage, conquery.getConfig());
		final User user  = new User("testUser", "testUserLabel");

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();

			storage.addUser(user);
			user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, dataset.getId()));
		}

		// Query cannot be deserialized without Namespace set up
		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());


		// Id of the lone concept that is used in the test.
		Concept<?> conceptId = conquery.getNamespace().getStorage().getAllConcepts().iterator().next();

		IntegrationUtils.assertQueryResult(conquery, query, -1, ExecutionState.FAILED, user, 403);

		// Add the necessary Permission
		{
			final ConqueryPermission permission = conceptId.createPermission(Ability.READ.asSet());
			log.info("Adding the Permission[{}] to User[{}]", permission, user);
			AuthorizationHelper.addPermission(user, permission, storage);
		}

		// Only assert permissions
		IntegrationUtils.assertQueryResult(conquery, query, -1, ExecutionState.DONE, user, 201);

		conquery.waitUntilWorkDone();
		// Clean up
		{
			storage.removeUser(user.getId());
		}
	}

}
