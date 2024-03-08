package com.bakdata.conquery.models.auth.entities;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.NonNull;

/**
 * An interface for classes that facade a user or represent a user.
 *
 * This interface allows realms to present a user different (usually reduced) set of permissions/abilities.
 * 
 **/
public interface Subject extends Principal {

	UserId getId();

	void authorize(@NonNull Authorized object, @NonNull Ability ability);

	void authorize(Set<? extends Authorized> objects, Ability ability);

	boolean isPermitted(Authorized object, Ability ability);

	boolean isPermittedAll(Collection<? extends Authorized> authorized, Ability ability);

	boolean[] isPermitted(List<? extends Authorized> authorized, Ability ability);

	boolean isOwner(Authorized object);

	boolean isDisplayLogout();

	ConqueryAuthenticationInfo getAuthenticationInfo();

	void setAuthenticationInfo(ConqueryAuthenticationInfo info);

	User getUser();
}
