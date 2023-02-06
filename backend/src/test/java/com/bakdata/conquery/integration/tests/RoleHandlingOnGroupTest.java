package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;

/**
 * Tests if roles are correctly added and removed from a subject.
 *
 */
public class RoleHandlingOnGroupTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {


	

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		MetaStorage storage = conquery.getMetaStorage();

		Group group1 = new Group("company", "company", storage);
		Role role = new Role("role1", "role1", storage);
		TestUser user1 = new TestUser(storage);

		try {
			storage.addRole(role);
			storage.addUser(user1);
			storage.addGroup(group1);
			role.addPermission(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")));

			//// Add user to group
			group1.addMember(user1);
			assertThat(user1.isPermitted(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")))).isFalse();
			
			//// Add role to group
			group1.addRole(role);
			assertThat(group1.getRoles()).containsExactlyInAnyOrder(role.getId());
			assertThat(user1.isPermitted(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")))).isTrue();

			
			//// Remove role from group
			group1.removeRole(role);
			assertThat(group1.getRoles()).isEmpty();
			assertThat(user1.isPermitted(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")))).isFalse();

		}
		finally {
			storage.removeGroup(group1.getId());
			storage.removeUser(user1.getId());
			storage.removeRole(role.getId());
		}

	}

}