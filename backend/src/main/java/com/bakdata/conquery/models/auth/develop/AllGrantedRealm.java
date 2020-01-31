package com.bakdata.conquery.models.auth.develop;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * This realm authenticates and authorizes all requests given to it positive.
 */
@Slf4j
public class AllGrantedRealm extends AuthorizingRealm implements ConqueryAuthenticationRealm {

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
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		return new DevelopmentAuthenticationInfo();		
	}
	
	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		return new DevelopmentToken();
	}
	
	@SuppressWarnings("serial")
	private static class DevelopmentToken implements AuthenticationToken{

		@Override
		public Object getPrincipal() {
			throw new UnsupportedOperationException(String.format("This is just a developer token, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public Object getCredentials() {
			throw new UnsupportedOperationException(String.format("This is just a developer token, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}
		
	}
	
	@SuppressWarnings("serial")
	private static class DevelopmentAuthenticationInfo implements AuthenticationInfo {

		@Override
		public PrincipalCollection getPrincipals() {
			return new DevelopmentPrincipalCollection();
		}

		@Override
		public Object getCredentials() {
			throw new UnsupportedOperationException(String.format("This is just a developer authentication info, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}
		
	}
	
	@SuppressWarnings("serial")
	private static class DevelopmentPrincipalCollection implements PrincipalCollection{

		@Override
		public Iterator iterator() {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public Object getPrimaryPrincipal() {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public <T> T oneByType(Class<T> type) {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public <T> Collection<T> byType(Class<T> type) {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public List asList() {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public Set asSet() {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public Collection fromRealm(String realmName) {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public Set<String> getRealmNames() {
			throw new UnsupportedOperationException(String.format("This is just a developer principal collection, which bypasses authentication when the %s is in use.", AllGrantedRealm.class.getName()));
		}

		@Override
		public boolean isEmpty() {
			return false;
		}
		
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		info.addObjectPermission(SuperPermission.onDomain());
		return info;
	}
}
