package com.bakdata.conquery.models.auth.util;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.*;

/**
 * Principal collection that carries a ready to use {@link Subject} object.
 */
@EqualsAndHashCode
public class UserLikePrincipalCollection implements PrincipalCollection {

	private static final long serialVersionUID = -1801050265305362978L;

	private final Subject principal;
	private final ConqueryAuthenticationRealm realm;

	public UserLikePrincipalCollection(@NonNull Subject subject, ConqueryAuthenticationRealm realm) {
		this.principal = subject;
		this.realm = realm;
	}

	@Override @JsonIgnore
	public Iterator<Subject> iterator() {
		return Collections.singleton(principal).iterator();
	}

	@Override @JsonIgnore
	public Subject getPrimaryPrincipal() {
		return principal;
	}

	@Override
	public <T> T oneByType(Class<T> type) {
		if(type.isAssignableFrom(principal.getClass())) {
			return (T) principal;
		}
		return null;
	}

	@Override
	public <T> Collection<T> byType(Class<T> type) {
		if(type.isAssignableFrom(principal.getClass())) {
			return List.of((T)principal);
		}
		return Collections.emptyList();
	}

	@Override
	public List<Subject> asList() {
		return  List.of(principal);
	}

	@Override
	public Set<Subject> asSet() {
		return Set.of(principal);
	}

	@Override
	public Collection<Subject> fromRealm(String realmName) {
		if(realm.getName().equals(realmName)){
			return List.of(principal);
		}
		return Collections.emptyList();
	}

	@Override @JsonIgnore
	public Set<String> getRealmNames() {
		if(realm != null) {
			return Set.of(realm.getName());
		}
		return Collections.emptySet();
	}

	@Override @JsonIgnore
	public boolean isEmpty() {
		return false;
	}

}
