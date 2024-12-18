package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class SuperPermissionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {


	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		Dataset dataset = conquery.getDataset();
		MetaStorage storage = conquery.getMetaStorage();

		Role role = new Role("company", "company", storage);
		TestUser user = new TestUser(storage);

		storage.addRole(role);

		try {
			user.addRole(role.getId());
			// Add SuperPermission to User
			user.addPermission(SuperPermission.onDomain());

			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset.getId()))).isTrue();
			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset.getId()))).isTrue();

			// Add SuperPermission to mandator and remove from user
			user.removePermission(SuperPermission.onDomain());
			role.addPermission(SuperPermission.onDomain());

			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset.getId()))).isTrue();
			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset.getId()))).isTrue();

			// Add SuperPermission to mandator and remove from user
			role.removePermission(SuperPermission.onDomain());

			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset.getId()))).isFalse();
			assertThat(user.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset.getId()))).isFalse();
		}
		finally {
			storage.removeUser(user.getId());
			storage.removeRole(role.getId());
		}

	}

}
