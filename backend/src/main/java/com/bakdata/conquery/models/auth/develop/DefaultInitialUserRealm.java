package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.extern.slf4j.Slf4j;
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
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
	}

	@Override
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof DevelopmentToken)) {
			return null;
		}
		DevelopmentToken devToken = (DevelopmentToken) token;
		return new ConqueryAuthenticationInfo(devToken.getPrincipal(), devToken.getCredentials(), this, true);
	}
}
