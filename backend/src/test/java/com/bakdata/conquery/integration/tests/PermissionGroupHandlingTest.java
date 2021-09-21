package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class PermissionGroupHandlingTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {


	/**
	 * This is a longer test that plays through different scenarios of permission
	 * and role adding/deleting. Creating many objects here to avoid side effects.
	 */
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MetaStorage storage = conquery.getMetaStorage();
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		ManagedExecutionId query1 = new ManagedExecutionId(dataset1.getId(), UUID.randomUUID());


		Role role1 = new Role("role", "role", storage::updateRole);
		TestUser user1 = new TestUser(storage);
		Group group1 = new Group("company", "company", storage::updateGroup);
		
		try {

			storage.addRole(role1);
			storage.addUser(user1);
			storage.addGroup(group1);

			user1.addRole(role1);

			group1.addMember(user1);

			user1.addPermission(ExecutionPermission.onInstance(Ability.READ, query1));
			role1.addPermission(ExecutionPermission.onInstance(Ability.DELETE, query1));
			group1.addPermission(ExecutionPermission.onInstance(Ability.SHARE, query1));

			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.READ, query1))).isTrue();
			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.DELETE, query1))).isTrue();
			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.SHARE, query1))).isTrue();
			
			// remove user from group
			group1.removeMember(user1);

			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.READ, query1))).isTrue();
			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.DELETE, query1))).isTrue();
			assertThat(user1.isPermitted(ExecutionPermission.onInstance(Ability.SHARE, query1))).isFalse();

		}
		finally {
			storage.removeGroup(group1.getId());
			storage.removeUser(user1.getId());
			storage.removeRole(role1.getId());
		}
	}

}
