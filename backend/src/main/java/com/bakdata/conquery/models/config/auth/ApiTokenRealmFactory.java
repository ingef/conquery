package com.bakdata.conquery.models.config.auth;

import java.nio.file.Path;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenCreator;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenRealm;
import com.bakdata.conquery.models.auth.apitoken.TokenStorage;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.resources.api.ApiTokenResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.util.Strings;
import lombok.Data;
import org.apache.http.util.CharArrayBuffer;
import org.apache.shiro.authc.AuthenticationToken;
import org.checkerframework.checker.nullness.qual.Nullable;

@CPSType(base = AuthenticationRealmFactory.class, id = "API_TOKEN")
@Data
public class ApiTokenRealmFactory implements AuthenticationRealmFactory {

	@NotNull
	private final Path storeDir;

	@NotNull
	private final XodusConfig apiTokenStoreConfig;

	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {

		final TokenStorage tokenStorage = new TokenStorage(storeDir, apiTokenStoreConfig, managerNode.getValidator(), Jackson.BINARY_MAPPER.copy());
		managerNode.getEnvironment().lifecycle().manage(tokenStorage);

		final ApiTokenRealm apiTokenRealm = new ApiTokenRealm(managerNode.getStorage(), tokenStorage);

		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(new ApiTokenExtractor());

		JerseyEnvironment environment = managerNode.getEnvironment().jersey();
		environment.register(apiTokenRealm);

		environment.register(ApiTokenResource.class);

		return apiTokenRealm;
	}

	public static class ApiTokenExtractor implements DefaultAuthFilter.TokenExtractor {

		@Override
		public @Nullable AuthenticationToken apply(@Nullable ContainerRequestContext input) {
			if (input == null) {
				return null;
			}

			// Unfortunately there is no way around the String here
			final String authHeader = input.getHeaderString(HttpHeaders.AUTHORIZATION);

			if (Strings.isNullOrEmpty(authHeader)) {
				return null;
			}

			String[] splits = authHeader.split(" ");
			if (splits.length != 2) {
				return null;
			}

			String token = splits[1];

			if (!token.startsWith(ApiTokenCreator.TOKEN_PREFIX)) {
				return null;
			}

			final CharArrayBuffer tokenBuf = new CharArrayBuffer(token.length());
			tokenBuf.append(token);
			return new ApiToken(tokenBuf);
		}
	}


}
