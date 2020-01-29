package com.bakdata.conquery.models.auth.develop;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryRealm;
import com.bakdata.conquery.models.auth.util.SingleAuthenticationInfo;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * This realm authenticates and authorizes all requests given to it positive.
 */
@Slf4j
@CPSType(id="ALL_GRANTED", base=ConqueryRealm.class)
public class AllGrantedRealm extends ConqueryRealm {

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
		super(null);
		log.warn(WARNING);
		this.setAuthenticationTokenClass(AuthenticationToken.class);
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(token instanceof UsernamePasswordToken) {
			// Authenticate every token as the superuser
			return new SingleAuthenticationInfo(new UserId((String)token.getPrincipal()), token.getCredentials());		
		}
		return null;
	}
	
	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		// TODO Auto-generated method stub
		return new UsernamePasswordToken(DevAuthConfig.USER.getId().getEmail(), new char[]{});
	}
}
