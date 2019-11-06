package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class SubjectTest implements ProgrammaticIntegrationTest, IntegrationTest.Simple {

	/**
	 * This is a longer test that plays through different scenarios of permission and role adding/deleting.
	 * Creating many objects here to avoid side effects.
	 */
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
		
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		
		// setup mandator
		Role mandator1 = new Role("company", "company");
		storage.addRole(mandator1);
		
		DatasetPermission datasetPermission1 = new DatasetPermission(Ability.READ.asSet(), dataset1.getId());
		DatasetPermission datasetPermission2 = new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId());
		
		// setup user
		User user1  = new User("user", "user");
		storage.addUser(user1);
		
		user1.addRole(storage, mandator1);
		user1.addRole(storage, mandator1);
		assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1);

		user1.addPermission(storage, datasetPermission1);
		mandator1.addPermission(storage, datasetPermission2);
		
		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isTrue();
		
		// Delete permission from mandator
		mandator1.removePermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));
		assertThat(mandator1.getPermissionsEffective()).isEmpty();
		
		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isFalse();
		
		// Add permission to user
		user1.addPermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isTrue();
		

		// Delete permission from mandator
		user1.removePermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));
		
		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isFalse();
		

		// Add permission to mandator, remove mandator from user
		mandator1.addPermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));
		user1.removeRole(storage, mandator1);

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isFalse();
		
		// Add mandator back to user
		user1.addRole(storage, mandator1);

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isTrue();
		
		// Delete all permissions from mandator and user
		user1.removePermission(storage, new DatasetPermission(Ability.READ.asSet(), dataset1.getId()));
		mandator1.removePermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isFalse();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isFalse();
		
		/////// SUPER_PERMISSION ///////
		// Add SuperPermission to User
		user1.addPermission(storage, new SuperPermission());

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isTrue();
		

		// Add SuperPermission to mandator and remove from user
		user1.removePermission(storage, new SuperPermission());
		mandator1.addPermission(storage, new SuperPermission());

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isTrue();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isTrue();

		// Add SuperPermission to mandator and remove from user
		mandator1.removePermission(storage, new SuperPermission());

		assertThat(user1.isPermitted(new DatasetPermission(Ability.READ.asSet(), dataset1.getId()))).isFalse();
		assertThat(user1.isPermitted(new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()))).isFalse();
		
		/////// PERMISSION AGGREGATION ///////
		user1.addPermission(storage, new DatasetPermission(Ability.READ.asSet(), dataset1.getId()));
		user1.addPermission(storage, new DatasetPermission(Ability.DELETE.asSet(), dataset1.getId()));
		user1.addPermission(storage, new SuperPermission());
		user1.addPermission(storage, new AdminPermission());
		
		assertThat(user1.getPermissionsCopy()).containsExactlyInAnyOrder(
			new DatasetPermission(Set.of(Ability.DELETE,Ability.READ), dataset1.getId()),
			new SuperPermission(),
			new AdminPermission());
	}

}
