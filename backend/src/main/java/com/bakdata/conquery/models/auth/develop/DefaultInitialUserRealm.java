package com.bakdata.conquery.models.auth.develop;

import java.util.Objects;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.models.auth.AuthorizationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * This realm authenticates requests as if they are effected by the first
 * initial user, that is found in the {@link AuthorizationConfig}, when no
 * identifying information was found in the request. If the realm was able to
 * parse a {@link UserId} from a request, it submits this id in an
 * {@link AuthenticationToken}.
 * 
 */
@Slf4j
public class DefaultInitialUserRealm extends ConqueryAuthenticationRealm {

	private static final String UID_QUERY_STRING_PARAMETER = "access_token";

	// Not allowed to be empty to take the first user as default
	private final User defaultUser = Objects.requireNonNull(ConqueryConfig.getInstance()
		.getAuthorization().getInitialUsers().get(0).getUser(), "There must be at least one initial user configured.");

	/**
	 * The warning that is displayed, when the realm is instantiated.
	 */
	private static final String WARNING = "\n"
		+ "           §§\n"
		+ "          §  §\n"
		+ "         §    §\n"
		+ "        §      §\n"
		+ "       §  §§§§  §       You instantiated and are probably using a Shiro realm\n"
		+ "      §   §§§§   §      that does not do any permission checks or authentication.\n"
		+ "     §     §§     §     Access to all resources is granted to everyone.\n"
		+ "    §      §§      §    DO NOT USE THIS REALM IN PRODUCTION\n"
		+ "   $                §\n"
		+ "  §        §§        §\n"
		+ " §                    §\n"
		+ " §§§§§§§§§§§§§§§§§§§§§§";

	/**
	 * Standard constructor.
	 */
	public DefaultInitialUserRealm() {
		log.warn(WARNING);
		this.setAuthenticationTokenClass(DevelopmentToken.class);
		this.setCredentialsMatcher(new SkippingCredentialsMatcher());
	}

	@Override
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof DevelopmentToken)) {
			return null;
		}
		DevelopmentToken devToken = (DevelopmentToken) token;
		return new ConqueryAuthenticationInfo(devToken.getPrincipal(), devToken.getCredentials(), this);
	}

	/**
	 * Tries to extract a plain {@link UserId} from the request to submit it for the authentication process.
	 */
	@Override
	public AuthenticationToken extractToken(ContainerRequestContext requestContext) {
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
