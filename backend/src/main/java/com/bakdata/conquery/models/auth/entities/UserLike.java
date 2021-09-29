package com.bakdata.conquery.models.auth.entities;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserLike extends Principal {

	UserId getId();

	public void authorize(@NonNull Authorized object, @NonNull Ability ability);

	public void authorize(Set<? extends Authorized> objects, Ability ability);

	public boolean isPermitted(Authorized object, Ability ability);

	public boolean isPermittedAll(Collection<? extends Authorized> authorized, Ability ability);

	public boolean[] isPermitted(List<? extends Authorized> authorizeds, Ability ability);

	public boolean isOwner(Authorized object);

	public boolean isDisplayLogout();

	public void setAuthenticationInfo(ConqueryAuthenticationInfo info);

	public User getUser();
}
