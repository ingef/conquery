package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

/**
 * Tests if roles are correctly added and removed from a subject.
 *
 */
public class RoleHandlingTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private final Role mandator1 = new Role("company", "company");
	private final Role mandator1Copy = new Role("company", "company");
	private final Role mandator2 = new Role("company2", "company2");
	private final User user1 = new User("user", "user");
	

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		MetaStorage storage = conquery.getMetaStorage();
		
		try {
			storage.addRole(mandator1);
			storage.addRole(mandator2);
			storage.addUser(user1);
			
			//// ADDING
			user1.addRole(storage, mandator1);
			assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1.getId());

			user1.addRole(storage, mandator1Copy);
			assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1.getId());

			user1.addRole(storage, mandator2);
			assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1.getId(), mandator2.getId());

			
			//// REMOVING
			user1.removeRole(storage, mandator2);
			assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1.getId());

			user1.removeRole(storage, mandator1);
			assertThat(user1.getRoles()).isEmpty();

		}
		finally {
			storage.removeUser(user1.getId());
			storage.removeRole(mandator1.getId());
			storage.removeRole(mandator2.getId());
		}

	}

}