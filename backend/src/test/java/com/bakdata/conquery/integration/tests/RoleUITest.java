package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MetaStorage;
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


	private MetaStorage storage;
	private Role mandator = new Role("testMandatorName", "testMandatorLabel");
	private RoleId mandatorId = mandator.getId();
	private User user = new User("testUser@test.de", "testUserName");
	private UserId userId = user.getId();
	private ConqueryPermission permission = DatasetPermission.onInstance(Ability.READ.asSet(), new DatasetId("testDatasetId"));

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		try {
	
			storage = conquery.getMetaStorage();
			storage.addRole(mandator);
			storage.addUser(user);
			// override permission object, because it might have changed by the subject
			// owning the permission
			permission = mandator.addPermission(storage, permission);
			user.addRole(storage, mandator);


			final UriBuilder root = UriBuilder.fromPath("admin")
											  .host("localhost")
											  .scheme("http")
											  .port(conquery.getAdminPort());


			URI classBase = HierarchyHelper.fromHierachicalPathResourceMethod(root, RoleUIResource.class, "getRole")
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
