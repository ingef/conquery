package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;

/**
 * Tests the mandator UI interface. Before the request is done, a mandator, a
 * user and a permission is created and stored. Then the request response is
 * tested against the created entities.
 *
 */
public class RoleUITest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MetaStorage storage = conquery.getMetaStorage();
		Role mandator = new Role("testMandatorName", "testMandatorLabel", storage);
		RoleId mandatorId = mandator.getId();
		User user = new User("testUser@test.de", "testUserName", storage);
		UserId userId = user.getId();
		try {

			ConqueryPermission permission = DatasetPermission.onInstance(Ability.READ.asSet(), new DatasetId("testDatasetId"));

			storage.addRole(mandator);
			storage.addUser(user);
			// override permission object, because it might have changed by the subject
			// owning the permission
			mandator.addPermission(permission);
			user.addRole(storage, mandator);


			URI classBase = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), RoleUIResource.class, "getRole")
				.buildFromMap(Map.of(ROLE_ID, mandatorId.toString()));
	
			Response response = conquery
				.getClient()
				.target(classBase)
				.request()
				.get();
	
			assertThat(response.getStatus()).isEqualTo(200);
			// Check for Freemarker Errors
			assertThat(response.readEntity(String.class).toLowerCase()).doesNotContain(List.of("freemarker", "debug"));

		}
		finally {
			storage.removeRole(mandatorId);
			storage.removeUser(userId);
		}
	}

}
