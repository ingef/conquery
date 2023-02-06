package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenRealm;
import com.bakdata.conquery.models.auth.apitoken.Scopes;
import com.bakdata.conquery.models.auth.conquerytoken.ConqueryTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.config.auth.ApiTokenRealmFactory;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetsResource;
import com.bakdata.conquery.resources.api.ApiTokenResource;
import com.bakdata.conquery.resources.api.DatasetsResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;

public class ApiTokenRealmTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public ConqueryConfig overrideConfig(ConqueryConfig conf, final File workDir) {

		XodusStoreFactory storageConfig = (XodusStoreFactory) conf.getStorage();
		final Path storageDir = workDir.toPath().resolve(storageConfig.getDirectory().resolve(getClass().getSimpleName()));

		return conf.withStorage(storageConfig.withDirectory(storageDir))
				   .withAuthenticationRealms(ImmutableList.<AuthenticationRealmFactory>builder()
														  .addAll(conf.getAuthenticationRealms())
														  .add(new ApiTokenRealmFactory(storageDir, new XodusConfig())).build())
				;

	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final User testUser = conquery.getTestUser();
		final ApiTokenRealm realm = conquery.getAuthorizationController().getAuthenticationRealms().stream()
									  .filter(ApiTokenRealm.class::isInstance)
									  .map(ApiTokenRealm.class::cast)
									  .collect(MoreCollectors.onlyElement());

		final ConqueryTokenRealm conqueryTokenRealm = conquery.getAuthorizationController().getConqueryTokenRealm();
		final String userToken = conqueryTokenRealm.createTokenForUser(testUser.getId());

		// Request ApiToken
		final ApiTokenDataRepresentation.Request tokenRequest1 = new ApiTokenDataRepresentation.Request();

		tokenRequest1.setName("test-token");
		tokenRequest1.setScopes(EnumSet.of(Scopes.DATASET));
		tokenRequest1.setExpirationDate(LocalDate.now().plus(1, ChronoUnit.DAYS));

		ApiToken apiToken1 = requestApiToken(conquery, userToken, tokenRequest1);

		assertThat(apiToken1.getToken()).isNotBlank();

		// List ApiToken
		List<ApiTokenDataRepresentation.Response> apiTokens =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class,"listUserTokens"))
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + userToken)
						.get(new GenericType<List<ApiTokenDataRepresentation.Response>>(){});

		final ApiTokenDataRepresentation.Response expected = new ApiTokenDataRepresentation.Response();
		expected.setLastUsed(null);
		expected.setCreationDate(LocalDate.now());
		expected.setExpirationDate(LocalDate.now().plus(1, ChronoUnit.DAYS));
		expected.setScopes(EnumSet.of(Scopes.DATASET));
		expected.setName("test-token");

		assertThat(apiTokens).hasSize(1);
		assertThat(apiTokens.get(0)).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);


		// Request ApiToken 2
		final ApiTokenDataRepresentation.Request tokenRequest2 = new ApiTokenDataRepresentation.Request();

		tokenRequest2.setName("test-token");
		tokenRequest2.setScopes(EnumSet.of(Scopes.ADMIN));
		tokenRequest2.setExpirationDate(LocalDate.now().plus(1, ChronoUnit.DAYS));

		ApiToken apiToken2 = requestApiToken(conquery, userToken, tokenRequest2);

		assertThat(apiToken2.getToken()).isNotBlank();

		// List ApiToken 2
		apiTokens = requestTokenList(conquery, userToken);

		assertThat(apiTokens).hasSize(2);

		// Use ApiToken1 to get Datasets
		List<IdLabel<DatasetId>> datasets = requestDatasets(conquery, apiToken1);

		assertThat(datasets).isNotEmpty();

		// Use ApiToken2 to get Datasets
		datasets = requestDatasets(conquery, apiToken2);

		assertThat(datasets).as("The second token has no scope for dataset").isEmpty();


		// Use ApiToken2 to access Admin
		List<DatasetId> adminDatasets =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetsResource.class,"listDatasets"))
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + apiToken2.getToken())
						.get(new GenericType<>() {});

		assertThat(adminDatasets).as("The second token has scope for admin").isNotEmpty();

		// Try to delete ApiToken2 with ApiToken (should fail)
		final UUID id2 = apiTokens.stream().filter(t -> t.getScopes().contains(Scopes.ADMIN)).map(ApiTokenDataRepresentation.Response::getId).collect(MoreCollectors.onlyElement());
		Response response =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class,"deleteToken"))
						.resolveTemplate(ApiTokenResource.TOKEN, id2)
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + apiToken2.getToken())
						.delete(Response.class);

		assertThat(response.getStatus()).as("It is forbidden to act on ApiTokens with ApiTokens").isEqualTo(403);


		// Delete ApiToken2 with user token
		response =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class,"deleteToken"))
						.resolveTemplate(ApiTokenResource.TOKEN, id2)
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + userToken)
						.delete(Response.class);

		assertThat(response.getStatus()).as("It is okay to act on ApiTokens with UserTokens").isEqualTo(200);
		assertThat(realm.listUserToken(testUser)).hasSize(1);

		// Try to use the deleted token to access Admin
		response = conquery.getClient()
				.target(HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetsResource.class,"listDatasets"))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header("Authorization", "Bearer " + apiToken2.getToken())
				.get(Response.class);

		assertThat(response.getStatus()).as("Cannot use deleted token").isEqualTo(401);

		// Try to act on tokens from another user
		final MetaStorage metaStorage = conquery.getMetaStorage();
		final User user2 = new User("TestUser2", "TestUser2", metaStorage);
		metaStorage.addUser(user2);
		final String user2Token = conqueryTokenRealm.createTokenForUser(user2.getId());

		// Try to delete ApiToken2 with ApiToken (should fail)
		final UUID id1 = apiTokens.stream().filter(t -> t.getScopes().contains(Scopes.DATASET)).map(ApiTokenDataRepresentation.Response::getId).collect(MoreCollectors.onlyElement());
		response =
				conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class,"deleteToken"))
						.resolveTemplate(ApiTokenResource.TOKEN, id1)
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + user2Token)
						.delete(Response.class);

		assertThat(response.getStatus()).as("It is forbidden to act on someone else ApiTokens").isEqualTo(403);

		// Request ApiToken 3 (expired)
		final ApiTokenDataRepresentation.Request tokenRequest3 = new ApiTokenDataRepresentation.Request();

		tokenRequest3.setName("test-token");
		tokenRequest3.setScopes(EnumSet.of(Scopes.DATASET));
		tokenRequest3.setExpirationDate(LocalDate.now().minus(1, ChronoUnit.DAYS));

		assertThatThrownBy(() -> requestApiToken(conquery, userToken, tokenRequest3)).as("Expiration date is in the past").isExactlyInstanceOf(ClientErrorException.class).hasMessageContaining("HTTP 422");

		// Craft expired token behind validation to simulate the use of an expired token
		ApiToken apiToken3 = realm.createApiToken(user2, tokenRequest3);

		assertThatThrownBy(() -> requestDatasets(conquery,apiToken3)).as("Expired token").isExactlyInstanceOf(NotAuthorizedException.class);
	}

	private List<IdLabel<DatasetId>> requestDatasets(StandaloneSupport conquery, ApiToken apiToken) {
		return conquery.getClient()
			   .target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetsResource.class, "getDatasets"))
			   .request(MediaType.APPLICATION_JSON_TYPE)
			   .header("Authorization", "Bearer " + apiToken.getToken())
			   .get(new GenericType<>() {
				});
	}

	private List<ApiTokenDataRepresentation.Response> requestTokenList(StandaloneSupport conquery, String userToken) {
		return conquery.getClient()
						.target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class, "listUserTokens"))
						.request(MediaType.APPLICATION_JSON_TYPE)
						.header("Authorization", "Bearer " + userToken)
						.get(new GenericType<>() {});
	}

	private ApiToken requestApiToken(StandaloneSupport conquery, String userToken, ApiTokenDataRepresentation.Request tokenRequest) {
		return conquery.getClient()
					   .target(HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ApiTokenResource.class, "createToken"))
					   .request(MediaType.APPLICATION_JSON_TYPE)
					   .header("Authorization", "Bearer " + userToken)
					   .post(Entity.entity(tokenRequest, MediaType.APPLICATION_JSON_TYPE), ApiToken.class);
	}
}
