package com.bakdata.conquery.models.auth.develop;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * This realm authenticates and authorizes all requests given to it positive.
 */
@Slf4j
public class AllGrantedRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {
	private static final String UID_QUERY_STRING_PARAMETER = "access_token";	

	// Not allowed to be empty to take the first user as default
	private final User defaultUser = ConqueryConfig.getInstance().getAuthorization().getInitialUsers().get(0).getUser();

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
	public AllGrantedRealm() {
		log.warn(WARNING);
		this.setAuthenticationTokenClass(DevelopmentToken.class);
		this.setCredentialsMatcher(new DevelopmentCredentialsMatcher());
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(!(token instanceof DevelopmentToken)) {
			return null;
		}
		DevelopmentToken devToken = (DevelopmentToken) token;
		return new ConqueryAuthenticationInfo(devToken.getPrincipal(), devToken.getCredentials(), this);
	}
	
	@Override
	public AuthenticationToken extractToken(ContainerRequestContext requestContext) {
		// Check if the developer passed a UserId under whose the Request should be executed 
		
		// Check the Authorization header for a String which can be parsed as a UserId
		String uid = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (uid == null) {
			// Check also the query parameter "access_token" for a UserId
			uid = requestContext.getUriInfo().getQueryParameters().getFirst(UID_QUERY_STRING_PARAMETER);
		}
		
		UserId userId = null;
		
		if (uid != null) {
			userId = UserId.Parser.INSTANCE.parse(uid);
		} else {
			// If nothing was found execute the request as the default user
			userId = defaultUser.getId();
		}
		return new DevelopmentToken(userId, uid);
	}
	
	private static class DevelopmentCredentialsMatcher implements CredentialsMatcher {

		@Override
		public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
			return true;
		}
		
	}
}
