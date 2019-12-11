package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validator;

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
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest implements ProgrammaticIntegrationTest {

	private Role mandator = new Role("99999998", "MANDATOR_LABEL");
	private Role deleteMandator = new Role("99999997", "SHOULD_BE_DELETED_MANDATOR");
	private User user = new User("user@test.email", "USER_LABEL");
	private Group group = new Group("groupName", "groupLabel");

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		Validator validator = Validators.newValidator();
		DatasetId dataset;
		ConqueryTestSpec test;
		PersistentIdMap persistentIdMap = IdMapSerialisationTest
			.createTestPersistentMap();


		try (StandaloneSupport conquery = testConquery.getSupport(name)) {
			dataset = conquery.getDataset().getId();

			test = JsonIntegrationTest.readJson(dataset, testJson);
			ValidatorHelper.failOnError(log, validator.validate(test));

			test.importRequiredData(conquery);

			test.executeTest(conquery);

			// Auth testing
			MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
			storage.addRole(mandator);
			storage.addRole(deleteMandator);
			storage.removeRole(deleteMandator.getId());

			// IDMapping Testing
			NamespaceStorage namespaceStorage = conquery.getStandaloneCommand().getMaster().getNamespaces().get(dataset).getStorage();


			namespaceStorage.updateIdMapping(persistentIdMap);

			storage.addUser(user);
			user.addRole(storage, mandator);

			storage.addGroup(group);
			group.addPermission(storage, DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset")));
			group.addMember(storage, user);

		}

		//stop dropwizard directly so ConquerySupport does not delete the tmp directory
		testConquery.getDropwizard().after();
		//restart
		testConquery.beforeAll(testConquery.getBeforeAllContext());

		try (StandaloneSupport conquery = testConquery.openDataset(dataset)) {
			test.executeTest(conquery);

			MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
			User userStored = storage.getUser(user.getId());
			Role mandatorStored = storage.getRole(mandator.getId());
			Role userRefMand = userStored.getRoles().iterator().next();
			assertThat(mandatorStored).isSameAs(userRefMand);
			assertThat(storage.getRole(deleteMandator.getId())).as("deleted mandator should stay deleted").isNull();

			// Check if user still is permitted to the permission from its group
			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.READ, new DatasetId("testDataset")))).isTrue();
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
