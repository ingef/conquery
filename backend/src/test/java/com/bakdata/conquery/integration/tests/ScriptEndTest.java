package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class ScriptEndTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final URI scriptUri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder()
															, AdminResource.class, "executeScript")
													.build();

		try(Response resp = conquery.getClient().target(scriptUri).request(MediaType.TEXT_PLAIN_TYPE).post(Entity.entity("storage", MediaType.TEXT_PLAIN_TYPE))){
			assertThat(resp.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);

			assertThat(resp.readEntity(String.class))
					.contains(MetaStorage.class.getSimpleName());
		}
	}
}
