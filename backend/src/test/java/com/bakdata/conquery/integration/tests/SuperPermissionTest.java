package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class SuperPermissionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private final Role role1 = new Role("company", "company");
	private final User user1 = new User("user", "user");
	

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		MetaStorage storage = conquery.getMetaStorage();
		storage.addRole(role1);
		
		try {
			user1.addRole(storage, role1);
			// Add SuperPermission to User
			user1.addPermission(storage,  SuperPermission.onDomain());
		
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset1.getId()))).isTrue();
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset1.getId()))).isTrue();
		
			// Add SuperPermission to mandator and remove from user
			user1.removePermission(storage, SuperPermission.onDomain());
			role1.addPermission(storage, SuperPermission.onDomain());
		
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset1.getId()))).isTrue();
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset1.getId()))).isTrue();
		
			// Add SuperPermission to mandator and remove from user
			role1.removePermission(storage, SuperPermission.onDomain());
		
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.READ, dataset1.getId()))).isFalse();
			assertThat(user1.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset1.getId()))).isFalse();
		}
		finally {
			storage.removeUser(user1.getId());
			storage.removeRole(role1.getId());
		}

	}

}
