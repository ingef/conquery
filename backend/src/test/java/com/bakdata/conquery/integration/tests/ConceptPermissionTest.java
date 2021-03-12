package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.query.IQuery;
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
		final IQuery query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());


		// Id of the lone concept that is used in the test.
		ConceptId conceptId = conquery.getNamespace().getStorage().getAllConcepts().iterator().next().getId();

		IntegrationUtils.assertQueryResult(conquery, query, -1, ExecutionState.FAILED, user, 401);

		// Add the necessary Permission
		{
			user.addPermission(storage, ConceptPermission.onInstance(Ability.READ, conceptId));			
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
