package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConceptPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthorizedException;

@Slf4j
public class ConceptPermissionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final MetaStorage storage = conquery.getMetaStorage();
		final Dataset dataset = conquery.getDataset();
		final String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset.getId(), testJson);
		final IQuery query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());
		final QueryProcessor processor = new QueryProcessor(storage.getDatasetRegistry(), storage);
		final User user  = new User("testUser", "testUserLabel");

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();

			storage.addUser(user);
			user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, dataset.getId()));
		}
		
		// Id of the lone concept that is used in the test.
		ConceptId conceptId = conquery.getNamespace().getStorage().getAllConcepts().iterator().next().getId();
		assertThatThrownBy(() -> {
			executeAndWaitUntilFinish(processor, dataset, query, user, storage);
		})
		.isInstanceOf(UnauthorizedException.class)
		.hasMessage(String.format("Subject does not have permission [%s]", ConceptPermission.onInstance(Ability.READ, conceptId)));

		// Add the necessary Permission
		{
			user.addPermission(storage, ConceptPermission.onInstance(Ability.READ, conceptId));			
		}
		
		assertThatCode(() ->{			
			executeAndWaitUntilFinish(processor, dataset, query, user, storage);
		}).doesNotThrowAnyException();
		
		conquery.waitUntilWorkDone();
		// Clean up
		{
			storage.removeUser(user.getId());
		}
	}
	
	public static void executeAndWaitUntilFinish(QueryProcessor processor, Dataset dataset, QueryDescription query, User user, MetaStorage storage ) {
		ExecutionStatus status = processor.postQuery(dataset, query, null, user);
		Objects.requireNonNull(storage.getExecution(status.getId()), "Execution was not found in storage, even though it was startet")
			.awaitDone(2, TimeUnit.MINUTES);
	}

}
