package com.bakdata.conquery.models.auth.apitoken;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.realm.AuthenticatingRealm;


/**
 * This realm provides and checks long-living API tokens. The tokens support a limited scope of actions that is backed
 * by the actual permissions of the invoking user.
 *
 */
@Slf4j
public class ApiTokenRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {

	private final MetaStorage storage;
	private final TokenStorage tokenStorage;

	private final ApiTokenCreator apiTokenCreator = new ApiTokenCreator();

	public ApiTokenRealm(MetaStorage storage, TokenStorage tokenStorage) {
		this.storage = storage;
		this.tokenStorage = tokenStorage;
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.setAuthenticationTokenClass(ApiToken.class);
	}


	@Override
	protected void onInit() {
		super.onInit();
	}


	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof ApiToken)) {
			return null;
		}

		final ApiToken apiToken = ((ApiToken) token);
		ApiTokenHash tokenHash = ApiTokenCreator.hashToken(apiToken);

		// Clear the token
		apiToken.clear();


		ApiTokenData tokenData = tokenStorage.get(tokenHash);
		if (tokenData == null) {
			log.trace("Unknown token, cannot map token hash to token data. Aborting authentication");
			throw new IncorrectCredentialsException();
		}

		final ApiTokenData.MetaData metaData = new ApiTokenData.MetaData(LocalDate.now());
		tokenStorage.updateMetaData(tokenData.getId(), metaData);

		final UserId userId = tokenData.getUserId();
		final User user = storage.getUser(userId);

		if (user == null) {
			throw new UnknownAccountException("The UserId does not map to a user: " + userId);
		}

		return new ConqueryAuthenticationInfo(new TokenScopedUser(user, tokenData), token, this, false);
	}



	public ApiToken createApiToken(User user, ApiTokenDataRepresentation.Request tokenRequest) {

		ApiToken token;

		synchronized (this) {
			ApiTokenHash hash;
			// Generate a token that does not collide with another tokens hash
			do {
				token = apiTokenCreator.createToken();
				hash = ApiTokenCreator.hashToken(token);

			} while(tokenStorage.get(hash) != null);

			final ApiTokenData apiTokenData = toInternalRepresentation(tokenRequest, user, hash, storage);

			tokenStorage.add(hash, apiTokenData);
		}

		return token;
	}

	public List<ApiTokenDataRepresentation.Response> listUserToken(Subject user) {
		ArrayList<ApiTokenDataRepresentation.Response> summary = new ArrayList<>();

		for (Iterator<Pair<ApiTokenData, ApiTokenData.MetaData>> it = tokenStorage.getAll(); it.hasNext(); ) {
			Pair<ApiTokenData, ApiTokenData.MetaData> apiToken = it.next();
			// Find all token data belonging to a user
			final ApiTokenData data = apiToken.getKey();
			if (!user.getId().equals(data.getUserId())){
				continue;
			}

			// Fill in the response with the details
			final ApiTokenDataRepresentation.Response response = new ApiTokenDataRepresentation.Response();
			response.setId(data.getId());
			response.setCreationDate(data.getCreationDate());
			response.setName(data.getName());
			response.setExpirationDate(data.getExpirationDate());
			response.setScopes(data.getScopes());

			// If the token was ever used it should have an meta data entry
			ApiTokenData.MetaData meta = apiToken.getValue();
			if (meta != null) {
				response.setLastUsed(meta.getLastUsed());
			}
			summary.add(response);
		}
		return summary;
	}

	public void deleteToken(@NotNull Subject user, @NonNull UUID tokenId) {

		Optional<ApiTokenData> tokenOpt = tokenStorage.getByUUID(tokenId);

		if (tokenOpt.isEmpty()) {
			log.warn("No token with id {} was found", tokenId);
			return;
		}


		final ApiTokenData token = tokenOpt.get();

		// Only the Owner or a user with admin capabilities can delete a token
		user.authorize(token, Ability.DELETE);

		tokenStorage.deleteToken(token);
	}



	private static ApiTokenData toInternalRepresentation(
			ApiTokenDataRepresentation.Request apiTokenRequest,
			User user,
			ApiTokenHash hash,
			MetaStorage storage) {
		return new ApiTokenData(
				UUID.randomUUID(),
				hash,
				apiTokenRequest.getName(),
				user.getId(),
				LocalDate.now(),
				apiTokenRequest.getExpirationDate(),
				apiTokenRequest.getScopes(),
				storage
		);
	}
}
