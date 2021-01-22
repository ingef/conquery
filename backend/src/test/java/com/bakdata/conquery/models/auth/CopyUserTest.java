package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.NonPersistentMetaStorage;
import org.junit.jupiter.api.Test;

public class CopyUserTest {

	@Test
	void testUserCopy(){
		MetaStorage storage = new NonPersistentMetaStorage();

		// Create test role
		Role role = new Role("role", "role");
		storage.addRole(role);
		role.addPermission(storage, DatasetPermission.onInstance(Ability.READ, new DatasetId("dataset0")));

		// Create test group
		Group group = new Group("group", "group");
		storage.addGroup(group);
		group.addPermission(storage, DatasetPermission.onInstance(Ability.READ, new DatasetId("dataset1")));

		// Create original user with role and group mapping
		User originUser = new User("user", "user");
		storage.addUser(originUser);
		originUser.addRole(storage, role);
		group.addMember(storage, originUser);

		// Do copy
		User copy = AuthorizationController.flatCopyUser(originUser.getId(), "copytest", storage);

		// Check that it is not the same user
		assertThat(copy).usingRecursiveComparison().isNotEqualTo(originUser);

		// Check that the copy does not have any mappings
		assertThat(group.containsMember(copy.getId())).isFalse();
		assertThat(copy.getRoles().contains(role.getId())).isFalse();

		// Check that the flat map worked
		assertThat(copy.getPermissions()).containsExactlyInAnyOrderElementsOf(AuthorizationHelper.getEffectiveUserPermissions(originUser.getId(),storage));

	}
}
