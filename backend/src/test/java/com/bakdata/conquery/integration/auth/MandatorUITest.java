package com.bakdata.conquery.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.IConqueryTest;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.TestConquery;

/**
 * Tests the mandator UI interface. Before the request is done, a mandator, a
 * user and a permission is created and stored. Then the request response is
 * tested against the created entities.
 *
 */
@CPSType(base = IConqueryTest.class, id = "HTTP_MANDATOR")
public class MandatorUITest implements IConqueryTest {

	private TestConquery conquery;

	private MasterMetaStorage storage;
	private MandatorId mandatorId = new MandatorId("testMandatorId");
	private Mandator mandator = new Mandator(new SinglePrincipalCollection(mandatorId));
	private UserId userId = new UserId("testUserId");
	private User user = new User(new SinglePrincipalCollection(userId));
	private ConqueryPermission permission = new DatasetPermission(null, Ability.READ.AS_SET, new DatasetId("testDatasetId"));

	@Override
	public void init(TestConquery conquery) {
		this.conquery = conquery;

		storage = conquery.getSupport().getStandaloneCommand().getMaster().getStorage();

		mandator.setName("testMandatorName");
		mandator.setLabel("testMandatorLabel");

		user.setName("testUserName");
		user.setLabel("testUserLabel");

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
	}

	@Override
	public void execute() {
		Response response = conquery
			.getClient()
			.target(String.format("http://localhost:%d/admin/mandators/%s", conquery.getDropwizard().getAdminPort(), mandatorId.toString()))
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

	@Override
	public void finish() {
		storage.removeMandator(mandatorId);
		storage.removeUser(userId);
		storage.removePermission(permission.getId());
	}

}
