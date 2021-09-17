package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.Scopes;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.config.auth.ApiTokenRealmFactory;
import com.bakdata.conquery.resources.api.ApiTokenResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiTokenRealmTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void overrideConfig(final ConqueryConfig conf, final File workDir) {

		XodusStoreFactory storageConfig = (XodusStoreFactory) conf.getStorage();
		final Path storageDir = workDir.toPath().resolve(storageConfig.getDirectory().resolve(this.getClass().getSimpleName()));
		storageConfig.setDirectory(storageDir);
		conf.getAuthenticationRealms().add(new ApiTokenRealmFactory(storageDir,new XodusConfig()));
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final User testUser = conquery.getTestUser();

		final String userToken = conquery.getAuthorizationController().getConqueryTokenRealm().createTokenForUser(testUser.getId());

		final ApiTokenDataRepresentation.Request tokenRequest = new ApiTokenDataRepresentation.Request();

		tokenRequest.setName("test-token");
		tokenRequest.setScopes(EnumSet.of(Scopes.DATASET));
		tokenRequest.setExpirationDate(LocalDate.now().plus(1, ChronoUnit.DAYS));

		// Request ApiToken
		ApiToken apiToken =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class,"createToken"))
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + userToken)
						.post(Entity.entity(tokenRequest, MediaType.APPLICATION_JSON_TYPE), ApiToken.class);

		assertThat(apiToken.getToken()).isNotBlank();



	}
}
