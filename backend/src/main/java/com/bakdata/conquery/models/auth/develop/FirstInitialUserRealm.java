package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * This realm authenticates requests as if they are effected by the first
 * initial user, that is found in the {@link AuthorizationConfig}, when no
 * identifying information was found in the request. If the realm was able to
 * parse a {@link UserId} from a request, it submits this id in an
 * {@link AuthenticationToken}.
 * 
 */
@Slf4j
@RequiredArgsConstructor
public class FirstInitialUserRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {


	public final MetaStorage storage;

	private final UserId defaultUserId;

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

	static {
		log.warn(WARNING);
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.setAuthenticationTokenClass(DevelopmentToken.class);
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
	}

	@Override
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(token instanceof DevelopmentToken devToken)) {
			return null;
		}
		final UserId userId = devToken.getPrincipal();

		if (userId == null) {
			final User defaultUser = getUserOrThrowUnknownAccount(storage, defaultUserId);
			return new ConqueryAuthenticationInfo(defaultUser, devToken.getCredentials(), this, true, null);
		}

		final User user = getUserOrThrowUnknownAccount(storage, userId);
		return new ConqueryAuthenticationInfo(user, devToken.getCredentials(), this, true, null);
	}
}
