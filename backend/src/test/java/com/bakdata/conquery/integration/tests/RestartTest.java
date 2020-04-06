package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validator;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest implements ProgrammaticIntegrationTest {

	private Role role = new Role("role", "ROLE");
	private Role roleToDelete = new Role("roleDelete", "ROLE_DELETE");
	private User user = new User("user@test.email", "USER");
	private User userToDelete = new User("userDelete@test.email", "USER_DELETE");
	private Group group = new Group("group", "GROUP");
	private Group groupToDelete = new Group("groupDelete", "GROUP_DELETE");

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		Validator validator = Validators.newValidator();
		DatasetId dataset;
		ConqueryTestSpec test;
		PersistentIdMap persistentIdMap = IdMapSerialisationTest
												  .createTestPersistentMap();

		MasterCommand master = testConquery.getStandaloneCommand().getMaster();
		AdminProcessor adminProcessor = new AdminProcessor(

				master.getConfig(),
				master.getStorage(),
				master.getNamespaces(),
				master.getJobManager(),
				master.getMaintenanceService(),
				master.getValidator()
		);


		try (StandaloneSupport conquery = testConquery.getSupport(name)) {
			dataset = conquery.getDataset().getId();

			test = JsonIntegrationTest.readJson(dataset, testJson);
			ValidatorHelper.failOnError(log, validator.validate(test));

			test.importRequiredData(conquery);

			test.executeTest(conquery);

			// IDMapping Testing
			NamespaceStorage namespaceStorage = conquery.getStandaloneCommand().getMaster().getNamespaces().get(dataset).getStorage();

			namespaceStorage.updateIdMapping(persistentIdMap);

			{// Auth testing (deletion and permission grant)
				// build constellation
				adminProcessor.addUser(user);
				adminProcessor.addUser(userToDelete);
				adminProcessor.addRole(role);
				adminProcessor.addRole(roleToDelete);
				adminProcessor.addGroup(group);
				adminProcessor.addGroup(groupToDelete);

				adminProcessor.addRoleTo(user.getId(), role.getId());
				adminProcessor.addRoleTo(user.getId(), roleToDelete.getId());
				adminProcessor.addRoleTo(userToDelete.getId(), role.getId());
				adminProcessor.addRoleTo(userToDelete.getId(), roleToDelete.getId());

				adminProcessor.addRoleTo(group.getId(), role.getId());
				adminProcessor.addRoleTo(group.getId(), roleToDelete.getId());
				adminProcessor.addRoleTo(groupToDelete.getId(), role.getId());
				adminProcessor.addRoleTo(groupToDelete.getId(), roleToDelete.getId());

				adminProcessor.addUserToGroup(group.getId(), user.getId());
				adminProcessor.addUserToGroup(group.getId(), userToDelete.getId());
				adminProcessor.addUserToGroup(groupToDelete.getId(), user.getId());
				adminProcessor.addUserToGroup(groupToDelete.getId(), userToDelete.getId());

				// Adding Permissions
				adminProcessor.createPermission(user.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset1")));
				adminProcessor.createPermission(userToDelete.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset2")));

				adminProcessor.createPermission(role.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset3")));
				adminProcessor.createPermission(roleToDelete.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset4")));

				adminProcessor.createPermission(group.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset5")));
				adminProcessor.createPermission(groupToDelete.getId(), DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset6")));

				// Delete entities
				adminProcessor.deleteUser(userToDelete.getId());
				adminProcessor.deleteRole(roleToDelete.getId());
				adminProcessor.deleteGroup(groupToDelete.getId());
			}

			testConquery.shutdown(conquery);

			//stop dropwizard directly so ConquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(testConquery.getBeforeAllContext());


			try (final StandaloneSupport support = testConquery.openDataset(dataset)) {

				test.executeTest(support);

				MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();

				{// Auth actual tests
					User userStored = storage.getUser(user.getId());
					assertThat(userStored).isEqualTo(user);
					assertThat(storage.getRole(role.getId())).isEqualTo(role);
					assertThat(storage.getGroup(group.getId())).isEqualTo(group);

					assertThat(storage.getUser(userToDelete.getId())).as("deleted user should stay deleted").isNull();
					assertThat(storage.getRole(roleToDelete.getId())).as("deleted role should stay deleted").isNull();
					assertThat(storage.getGroup(groupToDelete.getId())).as("deleted group should stay deleted").isNull();

					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset1")))).isTrue();
					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset2")))).isFalse(); // Was never permitted
					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset3")))).isTrue();
					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset4")))).isFalse(); // Was permitted by deleted role
					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset5")))).isTrue();
					assertThat(userStored.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset6")))).isFalse(); // Was permitted by deleted group

				}

				PersistentIdMap persistentIdMapAfterRestart = conquery.getStandaloneCommand()
																	  .getMaster()
																	  .getNamespaces()
																	  .get(dataset)
																	  .getStorage()
																	  .getIdMapping();
				assertThat(persistentIdMapAfterRestart).isEqualTo(persistentIdMap);
			}
		}
	}
}

