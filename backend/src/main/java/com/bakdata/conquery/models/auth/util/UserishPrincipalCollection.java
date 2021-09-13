package com.bakdata.conquery.models.auth.util;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Userish;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.*;

@EqualsAndHashCode
public class UserishPrincipalCollection implements PrincipalCollection {

	private static final long serialVersionUID = -1801050265305362978L;

	private final Userish principal;
	private final ConqueryAuthenticationRealm realm;

	public UserishPrincipalCollection(@NonNull Userish userish, ConqueryAuthenticationRealm realm) {
		this.principal = userish;
		this.realm = realm;
	}

	@Override @JsonIgnore
	public Iterator<Userish> iterator() {
		return new Iterator<>() {
			private boolean notCalled = true;
			@Override
			public boolean hasNext() {
				boolean ret = notCalled;
				notCalled = false;
				return ret;
			}

			@Override
			public Userish next() {
				return principal;
			}
		};
	}

	@Override @JsonIgnore
	public Userish getPrimaryPrincipal() {
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
	public List<Userish> asList() {
		return  List.of(principal);
	}

	@Override
	public Set<Userish> asSet() {
		return Set.of(principal);
	}

	@Override
	public Collection<Userish> fromRealm(String realmName) {
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
