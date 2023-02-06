package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.util.support.StandaloneSupport;

/**
 * Tests if Groups are correctly added and removed from a subject.
 *
 */
public class GroupHandlingTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {




	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MetaStorage storage = conquery.getMetaStorage();

		Group group1 = new Group("company", "company", storage);
		User user1 = new User("user", "user", storage);
		User user1copy = new User("user", "user", storage);
		User user2 = new User("user2", "user2", storage);

		try {
			storage.addGroup(group1);
			storage.addUser(user1);

			group1.addMember(user1);
			group1.addMember(user1copy);
			assertThat(group1.getMembers()).containsExactlyInAnyOrder(user1.getId());

			group1.addMember(user2);
			assertThat(group1.getMembers()).containsExactlyInAnyOrder(user1.getId(), user2.getId());

			group1.removeMember(user2);
			assertThat(group1.getMembers()).containsExactlyInAnyOrder(user1.getId());



		}
		finally {
			storage.removeUser(user1.getId());
			storage.removeUser(user2.getId());
			storage.removeGroup(group1.getId());
		}

	}

}