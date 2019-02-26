package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;

/**
 * Tests the mandator UI interface. Before the request is done, a mandator, a
 * user and a permission is created and stored. Then the request response is
 * tested against the created entities.
 *
 */
public class MandatorUITest implements IntegrationTest.Simple {

	private TestConquery conquery;

	private MasterMetaStorage storage;
	private Mandator mandator = new Mandator("testMandatorName", "testMandatorLabel");
	private MandatorId mandatorId = mandator.getId();
	private User user = new User("testUser@test.de", "testUserName");
	private UserId userId = user.getId();
	private ConqueryPermission permission = new DatasetPermission(null, Ability.READ.asSet(), new DatasetId("testDatasetId"));

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		try {
	
			storage = conquery.getStandaloneCommand().getMaster().getStorage();
			try {
				storage.addMandator(mandator);
				storage.addUser(user);
				// override permission object, because it might have changed by the subject
				// owning the permission
				permission = mandator.addPermission(storage, permission);
				user.addMandator(storage, mandator);
			}
			catch (JSONException e) {
				fail("Failed when adding to storage.",e);
			}
	
			Response response = conquery
				.getClient()
				.target(String.format("http://localhost:%d/admin/mandators/%s", conquery.getAdminPort(), mandatorId.toString()))
				.request()
				.get();
	
			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(response.readEntity(String.class))
				// check permission
				.contains(permission.getClass().getSimpleName(), permission.getTarget().toString())
				.containsSubsequence((Iterable<String>) () -> permission.getAbilities().stream().map(Enum::name).iterator())
				// check user
				.contains(user.getLabel());

		}
		finally {
			storage.removeMandator(mandatorId);
			storage.removeUser(userId);
			storage.removePermission(permission.getId());
		}
	}

}
