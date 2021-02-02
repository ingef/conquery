package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
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

	private final Group group1 = new Group("company", "company");
	private final Role role = new Role("role1", "role1");
	private final User user1 = new User("user", "user");
	

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		MetaStorage storage = conquery.getMetaStorage();
		
		try {
			storage.addRole(role);
			storage.addUser(user1);
			storage.addGroup(group1);
			role.addPermission(storage, new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")));

			//// Add user to group
			group1.addMember(storage, user1);
			assertThat(user1.isPermitted(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")))).isFalse();
			
			//// Add role to group
			group1.addRole(storage, role);
			assertThat(group1.getRoles()).containsExactlyInAnyOrder(role.getId());
			assertThat(user1.isPermitted(new DatasetPermission().instancePermission(Ability.READ, new DatasetId("testDataset")))).isTrue();

			
			//// Remove role from group
			group1.removeRole(storage, role);
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