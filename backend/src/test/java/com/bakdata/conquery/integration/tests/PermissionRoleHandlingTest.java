package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class PermissionRoleHandlingTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {



	/**
	 * This is a longer test that plays through different scenarios of permission
	 * and role adding/deleting. Creating many objects here to avoid side effects.
	 */
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MetaStorage storage = conquery.getMetaStorage();
		Role mandator1 = new Role("company", "company", storage::updateRole);
		TestUser user1 = new TestUser(storage);
		Dataset dataset = conquery.getDataset();

		try {

			storage.addRole(mandator1);
			storage.addUser(user1);

			user1.addRole(mandator1);

			user1.addPermission(dataset.createPermission(Ability.READ.asSet()));
			mandator1.addPermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isTrue();

			// Delete permission from mandator
			mandator1.removePermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));
			assertThat(mandator1.getPermissions()).isEmpty();

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isFalse();

			// Add permission to user
			user1.addPermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isTrue();

			// Delete permission from mandator
			user1.removePermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isFalse();

			// Add permission to mandator, remove mandator from user
			mandator1.addPermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));
			user1.removeRole(mandator1);

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isFalse();

			// Add mandator back to user
			user1.addRole(mandator1);

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isTrue();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isTrue();

			// Delete all permissions from mandator and user
			user1.removePermission(dataset.createPermission(Ability.READ.asSet()));
			mandator1.removePermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));

			assertThat(user1.isPermitted(dataset.createPermission(Ability.READ.asSet()))).isFalse();
			assertThat(user1.isPermitted(dataset.createPermission(Ability.DOWNLOAD.asSet()))).isFalse();
		}
		finally {
			storage.removeUser(user1.getId());
			storage.removeRole(mandator1.getId());
		}
	}

}
