package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import jakarta.inject.Named;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.jvnet.hk2.annotations.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@Named("user-id")
public class UserIdTokenExtractor implements DefaultAuthFilter.TokenExtractor {

	private static final String UID_QUERY_STRING_PARAMETER = "access_token";

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

		if (StringUtils.isEmpty(uid)) {
			// If nothing was found, submit an empty token that is recognized by the FirstInitialUserRealm to use the first initial user
			return new DevelopmentToken(null, uid);
		}

		try {
			userId = UserId.Parser.INSTANCE.parse(uid);
			log.trace("Parsed UserId: {}", userId);
			return new DevelopmentToken(userId, uid);
		} catch (Exception e) {
			log.trace("Unable to extract a valid user id.");
			return null;
		}
	}
}
