package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.execution.OverviewExecutionStatus;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RestartTest implements ProgrammaticIntegrationTest {

	public static final Dataset TEST_DATASET_1 = new Dataset("testDataset1");
	public static final Dataset TEST_DATASET_2 = new Dataset("testDataset2");
	public static final Dataset TEST_DATASET_3 = new Dataset("testDataset3");
	public static final Dataset TEST_DATASET_4 = new Dataset("testDataset4");
	public static final Dataset TEST_DATASET_5 = new Dataset("testDataset5");
	public static final Dataset TEST_DATASET_6 = new Dataset("testDataset6");

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {


		Validator validator = Validators.newValidator();

		ManagerNode manager = testConquery.getStandaloneCommand().getManagerNode();
		AdminDatasetProcessor adminDatasetProcessor = manager.getAdmin().getAdminDatasetProcessor();
		AdminProcessor adminProcessor = manager.getAdmin().getAdminProcessor();


		StandaloneSupport conquery = testConquery.getSupport(name);
		DatasetId dataset = conquery.getDataset().getId();

		log.info("Setup tests");
		ConqueryTestSpec test1 = setupTestQuery(dataset, validator, conquery, "/tests/query/RESTART_TEST_DATA/SIMPLE_FRONTEND_Query.json");
		ConqueryTestSpec test2 = setupTestQuery(dataset, validator, conquery, "/tests/query/MULTI_CONCEPT_SINGLE_TABLE/query.test.json");

		log.info("Execute tests");
		test1.executeTest(conquery);
		test2.executeTest(conquery);

		long numberOfExecutions;
		try(Stream<ManagedExecution> allExecutions = conquery.getMetaStorage().getAllExecutions()) {
			numberOfExecutions = allExecutions.count();
			assertThat(numberOfExecutions).isEqualTo(2);
		}

		// IDMapping Testing
		NamespaceStorage namespaceStorage = conquery.getNamespaceStorage();
		EntityIdMap entityIdMap = IdMapSerialisationTest.createTestPersistentMap(namespaceStorage);

		namespaceStorage.updateIdMapping(entityIdMap);


		final Dataset dataset1 = adminDatasetProcessor.addDataset(TEST_DATASET_1);
		final Dataset dataset2 = adminDatasetProcessor.addDataset(TEST_DATASET_2);
		final Dataset dataset3 = adminDatasetProcessor.addDataset(TEST_DATASET_3);
		final Dataset dataset4 = adminDatasetProcessor.addDataset(TEST_DATASET_4);
		final Dataset dataset5 = adminDatasetProcessor.addDataset(TEST_DATASET_5);
		final Dataset dataset6 = adminDatasetProcessor.addDataset(TEST_DATASET_6);


		MetaStorage storage = conquery.getMetaStorage();

		Role role = new Role("role", "ROLE", storage);
		Role roleToDelete = new Role("roleDelete", "ROLE_DELETE", storage);
		User user = new User("user@test.email", "USER", storage);
		User userToDelete = new User("userDelete@test.email", "USER_DELETE", storage);
		Group group = new Group("group", "GROUP", storage);
		Group groupToDelete = new Group("groupDelete", "GROUP_DELETE", storage);

		{// Auth testing (deletion and permission grant)
			// build constellation
			//TODO USE APIS

			adminProcessor.addUser(user);
			adminProcessor.addUser(userToDelete);
			adminProcessor.addRole(role);
			adminProcessor.addRole(roleToDelete);
			adminProcessor.addGroup(group);
			adminProcessor.addGroup(groupToDelete);

			adminProcessor.addRoleTo(user, role.getId());
			adminProcessor.addRoleTo(user, roleToDelete.getId());
			adminProcessor.addRoleTo(userToDelete, role.getId());
			adminProcessor.addRoleTo(userToDelete, roleToDelete.getId());

			adminProcessor.addRoleTo(group, role.getId());
			adminProcessor.addRoleTo(group, roleToDelete.getId());
			adminProcessor.addRoleTo(groupToDelete, role.getId());
			adminProcessor.addRoleTo(groupToDelete, roleToDelete.getId());

			adminProcessor.addUserToGroup(group.getId(), user.getId());
			adminProcessor.addUserToGroup(group.getId(), userToDelete.getId());
			adminProcessor.addUserToGroup(groupToDelete.getId(), user.getId());
			adminProcessor.addUserToGroup(groupToDelete.getId(), userToDelete.getId());

			// Adding Permissions
			adminProcessor.createPermission(user.getId(), dataset1.createPermission(Ability.READ.asSet()));
			adminProcessor.createPermission(userToDelete.getId(), dataset2.createPermission(Ability.READ.asSet()));

			adminProcessor.createPermission(role.getId(), dataset3.createPermission(Ability.READ.asSet()));
			adminProcessor.createPermission(roleToDelete.getId(), dataset4.createPermission(Ability.READ.asSet()));

			adminProcessor.createPermission(group.getId(), dataset5.createPermission(Ability.READ.asSet()));
			adminProcessor.createPermission(groupToDelete.getId(), dataset6.createPermission(Ability.READ.asSet()));

			// Delete entities
			//TODO use API
			adminProcessor.deleteUser(userToDelete.getId());
			adminProcessor.deleteRole(roleToDelete.getId());
			adminProcessor.deleteGroup(groupToDelete.getId());
		}

		log.info("Shutting down for restart");

		testConquery.shutdown();

		log.info("Restarting");
		testConquery.beforeAll();

		final StandaloneSupport support = testConquery.openDataset(dataset);


		log.info("Restart complete");

		DatasetRegistry<?> datasetRegistry = support.getDatasetRegistry();

		try(Stream<ManagedExecution> allExecutions = support.getMetaStorage().getAllExecutions()) {
			assertThat(allExecutions.count()).as("Executions after restart").isEqualTo(numberOfExecutions);
		}

		List<OverviewExecutionStatus> allQueries = IntegrationUtils.getAllQueries(support, 200);
		assertThat(allQueries).size().isEqualTo(1);

		log.info("Reexecute tests after restart.");
		test1.executeTest(support);
		test2.executeTest(support);

		storage = support.getMetaStorage();

		{// Auth actual tests
			User userStored = storage.getUser(user.getId());
			assertThat(userStored).isEqualTo(user);
			assertThat(storage.getRole(role.getId())).isEqualTo(role);
			assertThat(storage.getGroup(group.getId())).isEqualTo(group);

			assertThat(storage.getUser(userToDelete.getId())).as("deleted user should stay deleted").isNull();
			assertThat(storage.getRole(roleToDelete.getId())).as("deleted role should stay deleted").isNull();
			assertThat(storage.getGroup(groupToDelete.getId())).as("deleted group should stay deleted").isNull();

			assertThat(userStored.isPermitted(datasetRegistry.get(TEST_DATASET_1.getId()).getDataset(), Ability.READ)).isTrue();
			assertThat(userStored.isPermitted(datasetRegistry
													 .get(TEST_DATASET_2.getId())
													 .getDataset(), Ability.READ)).isFalse(); // Was never permitted
			assertThat(userStored.isPermitted(datasetRegistry.get(TEST_DATASET_3.getId()).getDataset(), Ability.READ)).isTrue();
			assertThat(userStored.isPermitted(datasetRegistry
													 .get(TEST_DATASET_4.getId())
													 .getDataset(), Ability.READ)).isFalse(); // Was permitted by deleted role
			assertThat(userStored.isPermitted(datasetRegistry.get(TEST_DATASET_5.getId()).getDataset(), Ability.READ)).isTrue();
			assertThat(userStored.isPermitted(datasetRegistry
													 .get(TEST_DATASET_6.getId())
													 .getDataset(), Ability.READ)).isFalse(); // Was permitted by deleted group

		}

		EntityIdMap entityIdMapAfterRestart = conquery.getNamespaceStorage()
													  .getIdMapping();
		assertThat(entityIdMapAfterRestart).isEqualTo(entityIdMap);

		// We need to reassign the dataset processor because the instance prio to the restart became invalid
		adminDatasetProcessor = testConquery.getStandaloneCommand().getManagerNode().getAdmin().getAdminDatasetProcessor();
		// Cleanup
		adminDatasetProcessor.deleteDataset(dataset1);
		adminDatasetProcessor.deleteDataset(dataset2);
		adminDatasetProcessor.deleteDataset(dataset3);
		adminDatasetProcessor.deleteDataset(dataset4);
		adminDatasetProcessor.deleteDataset(dataset5);
		adminDatasetProcessor.deleteDataset(dataset6);
	}

	private static @NotNull ConqueryTestSpec setupTestQuery(DatasetId dataset, Validator validator, StandaloneSupport conquery, String testPath) throws Exception {
		//read test specification
		String testJson = In.resource(testPath).withUTF8().readAll();
		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, validator.validate(test));

		test.importRequiredData(conquery);
		return test;
	}
}
