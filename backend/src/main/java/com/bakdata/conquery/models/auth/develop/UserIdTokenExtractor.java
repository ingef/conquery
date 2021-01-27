package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

@RequiredArgsConstructor
public class UserIdTokenExtractor implements DefaultAuthFilter.TokenExtractor {

	private static final String UID_QUERY_STRING_PARAMETER = "access_token";

	private final User defaultUser;

	/**
	 * Tries to extract a plain {@link UserId} from the request to submit it for the authentication process.
	 */
	@Override
	public AuthenticationToken apply(ContainerRequestContext requestContext) {
		// Check if the developer passed a UserId under whose the Request should be
		// executed

		// Check the Authorization header for a String which can be parsed as a UserId
		String uid = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (uid != null) {
			uid = uid.replaceFirst("^Bearer ", "");
		}
		else {
			// Check also the query parameter "access_token" for a UserId
			uid = requestContext.getUriInfo().getQueryParameters().getFirst(UID_QUERY_STRING_PARAMETER);
		}


		UserId userId = null;

		if (StringUtils.isNotEmpty(uid)) {
			uid = uid.replaceFirst("^Bearer ", "");

			try {
				userId = UserId.Parser.INSTANCE.parse(uid);
				return new DevelopmentToken(userId, uid);
			} catch (Exception e) {
				// do default
			}
		}
		// If nothing was found execute the request as the default user
		userId = defaultUser.getId();
		return new DevelopmentToken(userId, uid);
	}
}
