package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * This subject combines a {@link User} with a belonging {@link ApiTokenData}. Permissions for an authorization request are filtered based on the {@link Scopes} of the token.
 * If a permission is covered by a scope the authorization is delegated to the actual user.
 */
@RequiredArgsConstructor
@Getter
public class TokenScopedUser implements Subject {

	private final User delegate;
	private final ApiTokenData tokenContext;

	@Override
	public UserId getId() {
		return delegate.getId();
	}

	@Override
	public void authorize(@NonNull Authorized object, @NonNull Ability ability) {
		final ConqueryPermission permission = object.createPermission(EnumSet.of(ability));
		if(!tokenContext.isCoveredByScopes(permission)) {
				throw new UnauthorizedException("The scopes of the token do not support handling the permission: " + permission);
		}
		delegate.authorize(object,ability);
	}

	@Override
	public void authorize(Set<? extends Authorized> objects, Ability ability) {
		final EnumSet<Ability> abilityEnumSet = EnumSet.of(ability);
		if(!objects.stream().map(o -> o.createPermission(abilityEnumSet)).allMatch(tokenContext::isCoveredByScopes)) {
			throw new UnauthorizedException("The scopes of the tokens do not support handling the permission");
		}
		delegate.authorize(objects, ability);
	}

	@Override
	public boolean isPermitted(Authorized object, Ability ability) {
		final ConqueryPermission permission = object.createPermission(EnumSet.of(ability));
		if(!tokenContext.isCoveredByScopes(permission)) {
			return false;
		}
		return delegate.isPermitted(object, ability);
	}

	@Override
	public boolean isPermittedAll(Collection<? extends Authorized> authorized, Ability ability) {
		final EnumSet<Ability> abilitySet = EnumSet.of(ability);
		if(!authorized.stream().map(o -> o.createPermission(abilitySet)).allMatch(tokenContext::isCoveredByScopes)) {
			return false;
		}
		return delegate.isPermittedAll(authorized,ability);
	}

	@Override
	public boolean[] isPermitted(List<? extends Authorized> authorized, Ability ability) {
		final EnumSet<Ability> abilitySet = EnumSet.of(ability);

		boolean[] ret = new boolean[authorized.size()];
		for (int i = 0; i < ret.length; i++) {
			Authorized object = authorized.get(i);
			ret[i] = tokenContext.isCoveredByScopes(object.createPermission(abilitySet)) &&
					 delegate.isPermitted(object, ability);
		}
		return ret;
	}

	@Override
	public boolean isOwner(Authorized object) {
		return delegate.isOwner(object);
	}

	@Override
	public boolean isDisplayLogout() {
		return false;
	}

	@Override
	public void setAuthenticationInfo(ConqueryAuthenticationInfo info) {
		delegate.setAuthenticationInfo(info);
	}

	@Override
	public User getUser() {
		return delegate;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}
}
